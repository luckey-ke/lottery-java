package com.lottery.service.impl;

import com.lottery.entity.LotteryType;
import com.lottery.service.FetchService;
import com.lottery.service.impl.fetcher.FetchProgressCallback;
import com.lottery.service.impl.fetcher.ZhcwHtmlFetcher;
import com.lottery.service.impl.fetcher.ZhcwJsonFetcher;
import com.lottery.service.impl.task.FetchTaskManager;
import com.lottery.service.impl.task.FetchTaskManager.FetchTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 数据采集服务 — 职责：调度各 fetcher，管理异步任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchServiceImpl implements FetchService {

    private static final int FETCH_ALL_THREAD_COUNT = 6;

    private final ZhcwHtmlFetcher zhcwHtmlFetcher;
    private final ZhcwJsonFetcher zhcwJsonFetcher;
    private final FetchTaskManager taskManager;

    private ExecutorService taskExecutor;

    @PostConstruct
    public void init() {
        taskExecutor = Executors.newFixedThreadPool(8, r -> {
            Thread t = new Thread(r, "lottery-fetch-task");
            t.setDaemon(true);
            return t;
        });
        taskManager.recoverUnfinishedTasks();
    }

    @PreDestroy
    public void destroy() {
        if (taskExecutor != null) {
            log.info("[关闭] 正在关闭抓取任务线程池...");
            taskExecutor.shutdown();
        }
    }

    // ===== 对外接口 =====

    @Override
    public Map<String, Object> fetchAll(String scope, Integer count) {
        return runFetchAll(resolveScope(scope, count));
    }

    @Override
    public Map<String, Object> fetch(String lotteryType, String scope, Integer count) {
        return runFetch(lotteryType, resolveScope(scope, count), null);
    }

    @Override
    public Map<String, Object> startFetchAllTask(String scope, Integer count) {
        FetchScope fs = resolveScope(scope, count);
        FetchTask task = taskManager.createTask("all", fs.scope(), "concurrent-by-type", "manual", LotteryType.values().length);
        taskExecutor.submit(() -> executeAllTask(task, fs));
        return task.toMap();
    }

    @Override
    public Map<String, Object> startFetchTask(String lotteryType, String scope, Integer count) {
        FetchScope fs = resolveScope(scope, count);
        FetchTask task = taskManager.createTask(lotteryType, fs.scope(), "single", "manual", 1);
        taskExecutor.submit(() -> executeSingleTask(task, lotteryType, fs));
        return task.toMap();
    }

    @Override
    public Map<String, Object> getFetchTask(String taskId) {
        FetchTask task = taskManager.getTask(taskId);
        if (task == null) {
            return Map.of("taskId", taskId, "status", "not_found", "error", "任务不存在");
        }
        return task.toMap();
    }

    @Override
    public Map<String, Object> listFetchHistory(String status, String triggerSource, String type, int limit, int offset) {
        return fetchHistoryService.list(status, triggerSource, type, limit, offset);
    }

    @Override
    public Map<String, Object> getFetchHistory(String taskId) {
        return fetchHistoryService.detail(taskId);
    }

    @Override
    public void fetchLatest() {
        FetchScope fs = resolveScope("latest-1", 1);
        FetchTask task = taskManager.createTask("all", fs.scope(), "concurrent-by-type", "scheduled", LotteryType.values().length);
        taskExecutor.submit(() -> executeAllTask(task, fs));
    }

    // ===== 任务执行 =====

    private void executeSingleTask(FetchTask task, String lotteryType, FetchScope scope) {
        try {
            task.markRunning(lotteryType);
            taskManager.persist(task);
            Map<String, Object> result = runFetch(lotteryType, scope, task);
            task.markCompleted(String.valueOf(result.get("status")), result);
            taskManager.persist(task);
        } catch (Exception e) {
            log.error("抓取任务 [{}] 执行失败: {}", task.getTaskId(), e.getMessage(), e);
            task.markFailed(e.getMessage());
            taskManager.persist(task);
        }
    }

    private void executeAllTask(FetchTask task, FetchScope scope) {
        try {
            task.markRunning("all");
            taskManager.persist(task);
            Map<String, Object> result = runFetchAll(scope, task);
            task.markCompleted(String.valueOf(result.get("status")), result);
            taskManager.persist(task);
        } catch (Exception e) {
            log.error("抓取全部任务 [{}] 执行失败: {}", task.getTaskId(), e.getMessage(), e);
            task.markFailed(e.getMessage());
            taskManager.persist(task);
        }
    }

    // ===== 核心调度 =====

    private Map<String, Object> runFetchAll(FetchScope scope, FetchTask task) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("scope", scope.scope());
        summary.put("mode", "concurrent-by-type");

        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(FETCH_ALL_THREAD_COUNT, LotteryType.values().length));
        Map<LotteryType, Future<int[]>> futures = new LinkedHashMap<>();
        boolean hasError = false;
        int totalFetched = 0, inserted = 0, updated = 0;

        try {
            for (LotteryType lt : LotteryType.values()) {
                futures.put(lt, executor.submit(() -> {
                    Map<String, Object> r = runFetch(lt.getCode(), scope, task);
                    return new int[]{
                        asInt(r.get("totalFetched")),
                        asInt(r.get("inserted")),
                        asInt(r.get("updated"))
                    };
                }));
            }
            for (Map.Entry<LotteryType, Future<int[]>> entry : futures.entrySet()) {
                try {
                    int[] r = entry.getValue().get();
                    totalFetched += r[0];
                    inserted += r[1];
                    updated += r[2];
                } catch (Exception e) {
                    hasError = true;
                    log.error("拉取 [{}] 异常: {}", entry.getKey().getCode(), e.getMessage(), e);
                }
            }
        } finally {
            executor.shutdown();
        }

        summary.put("status", hasError ? "partial_failed" : "success");
        summary.put("totalFetched", totalFetched);
        summary.put("inserted", inserted);
        summary.put("updated", updated);
        return summary;
    }

    private Map<String, Object> runFetch(String lotteryType, FetchScope scope, FetchTask task) {
        log.info("开始拉取 [{}] 真实数据, scope={}", lotteryType, scope.scope());

        FetchProgressCallback progressCallback = (page, total, ins, upd) -> {
            if (task != null) task.updateProgress(lotteryType, page, total, ins, upd);
            if (task != null) taskManager.persist(task);
        };

        int[] stats;
        try {
            stats = switch (lotteryType) {
                case "dlt", "pl3", "pl5" -> zhcwJsonFetcher.fetch(
                        lotteryType, scope.limitCount(), scope.cutoffDate(), scope.all(), progressCallback);
                case "ssq", "fc3d", "qlc" -> zhcwHtmlFetcher.fetch(
                        lotteryType, scope.limitCount(), scope.cutoffDate(), scope.all(), progressCallback);
                default -> throw new IllegalArgumentException("不支持的彩种: " + lotteryType);
            };
        } catch (Exception e) {
            log.warn("[{}] 真实数据拉取失败: {}", lotteryType, e.getMessage());
            Map<String, Object> errorResult = buildResult(lotteryType, scope.scope(), 0, 0, 0, e.getMessage());
            if (task != null) task.recordTypeResult(lotteryType, errorResult);
            return errorResult;
        }

        log.info("[{}] 真实数据完成: 获取 {} 条, 新增 {} 条, 更新 {} 条",
                lotteryType, stats[0], stats[1], stats[2]);

        Map<String, Object> result = buildResult(lotteryType, scope.scope(), stats[0], stats[1], stats[2], null);
        if (task != null) task.recordTypeResult(lotteryType, result);
        return result;
    }

    // ===== Scope 解析 =====

    private FetchScope resolveScope(String scope, Integer count) {
        if (count != null && count > 0) {
            return new FetchScope("latest-" + count, count, null, false);
        }
        String normalized = (scope == null || scope.isBlank()) ? "latest-1" : scope.trim();
        return switch (normalized) {
            case "latest-1" -> new FetchScope(normalized, 1, null, false);
            case "latest-10" -> new FetchScope(normalized, 10, null, false);
            case "latest-50" -> new FetchScope(normalized, 50, null, false);
            case "latest-100" -> new FetchScope(normalized, 100, null, false);
            case "year-1" -> new FetchScope(normalized, null, LocalDate.now().minusYears(1).toString(), false);
            case "year-3" -> new FetchScope(normalized, null, LocalDate.now().minusYears(3).toString(), false);
            case "all" -> new FetchScope(normalized, null, null, true);
            default -> throw new IllegalArgumentException("不支持的 scope: " + normalized);
        };
    }

    // ===== 辅助 =====

    private Map<String, Object> buildResult(String type, String scope, int total, int ins, int upd, String error) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("type", type);
        r.put("name", resolveName(type));
        r.put("scope", scope);
        r.put("status", error != null ? "failed" : "success");
        r.put("totalFetched", total);
        r.put("total", total);
        r.put("inserted", ins);
        r.put("new", ins);
        r.put("updated", upd);
        if (error != null) r.put("error", error);
        return r;
    }

    private String resolveName(String type) {
        try { return LotteryType.fromCode(type).getName(); } catch (Exception e) { return type; }
    }

    private int asInt(Object v) {
        if (v instanceof Number n) return n.intValue();
        if (v == null) return 0;
        return Integer.parseInt(String.valueOf(v));
    }

    private record FetchScope(String scope, Integer limitCount, String cutoffDate, boolean all) {}
}
