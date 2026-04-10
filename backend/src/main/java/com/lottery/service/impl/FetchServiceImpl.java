package com.lottery.service.impl;

import com.lottery.entity.LotteryType;
import com.lottery.service.FetchHistoryService;
import com.lottery.service.FetchService;
import com.lottery.service.impl.fetcher.FetchProgressCallback;
import com.lottery.service.impl.fetcher.ZhcwHtmlFetcher;
import com.lottery.service.impl.fetcher.ZhcwJsonFetcher;
import com.lottery.service.impl.task.FetchTaskManager;
import com.lottery.service.impl.task.FetchTaskManager.FetchTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 数据采集服务 — 职责：调度各 fetcher，管理异步任务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FetchServiceImpl implements FetchService {

    private static final int FETCH_ALL_THREAD_COUNT = 6;
    private static final DateTimeFormatter TASK_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FetchHistoryService fetchHistoryService;
    private final ZhcwHtmlFetcher zhcwHtmlFetcher;
    private final ZhcwJsonFetcher zhcwJsonFetcher;
    private final FetchTaskManager taskManager;

    @Qualifier("lotteryFetchExecutor")
    private final ThreadPoolTaskExecutor taskExecutor;

    @PostConstruct
    public void init() {
        taskManager.recoverUnfinishedTasks();
    }

    @PreDestroy
    public void destroy() {
        log.info("[关闭] 正在关闭抓取任务线程池...");
        taskExecutor.shutdown();
        try {
            if (!taskExecutor.getThreadPoolExecutor().awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("[关闭] 线程池超时，强制关闭");
                taskExecutor.shutdown();
            }
        } catch (InterruptedException e) {
            taskExecutor.shutdown();
            Thread.currentThread().interrupt();
        }
    }

    // ===== 对外接口 =====

    @Override
    public Map<String, Object> fetchAll(String scope, Integer count) {
        return runFetchAll(resolveScope(scope, count), null);
    }

    @Override
    public Map<String, Object> fetch(String lotteryType, String scope, Integer count) {
        return runFetch(lotteryType, resolveScope(scope, count), null);
    }

    @Override
    public Map<String, Object> startFetchAllTask(String scope, Integer count) {
        FetchScope fetchScope = resolveScope(scope, count);
        FetchTask task = taskManager.createTask("all", fetchScope.getScope(), "concurrent-by-type", "manual", LotteryType.values().length);
        taskExecutor.execute(() -> executeAllTask(task, fetchScope));
        return task.toMap();
    }

    @Override
    public Map<String, Object> startFetchTask(String lotteryType, String scope, Integer count) {
        FetchScope fetchScope = resolveScope(scope, count);
        FetchTask task = taskManager.createTask(lotteryType, fetchScope.getScope(), "single", "manual", 1);
        taskExecutor.execute(() -> executeSingleTask(task, lotteryType, fetchScope));
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
        FetchScope fetchScope = resolveScope("latest-1", 1);
        FetchTask task = taskManager.createTask("all", fetchScope.getScope(), "concurrent-by-type", "scheduled", LotteryType.values().length);
        taskExecutor.execute(() -> executeAllTask(task, fetchScope));
    }

    // ===== 任务执行 =====

    private void executeSingleTask(FetchTask task, String lotteryType, FetchScope scope) {
        try {
            task.markRunning(lotteryType);
            taskManager.persist(task);
            Map<String, Object> result = runFetch(lotteryType, scope, task);
            task.markCompleted(stringValue(result.get("status")), result);
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
            task.markCompleted(stringValue(result.get("status")), result);
            taskManager.persist(task);
        } catch (Exception e) {
            log.error("抓取全部任务 [{}] 执行失败: {}", task.getTaskId(), e.getMessage(), e);
            task.markFailed(e.getMessage());
            taskManager.persist(task);
        }
    }

    // ===== 核心调度 =====

    private Map<String, Object> runFetchAll(FetchScope fetchScope, FetchTask task) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("scope", fetchScope.getScope());
        summary.put("mode", "concurrent-by-type");

        ExecutorService executor = Executors.newFixedThreadPool(
                Math.min(FETCH_ALL_THREAD_COUNT, LotteryType.values().length));
        Map<LotteryType, Future<Map<String, Object>>> futures = new LinkedHashMap<>();
        boolean hasError = false;
        int totalFetched = 0;
        int inserted = 0;
        int updated = 0;

        try {
            for (LotteryType lotteryType : LotteryType.values()) {
                futures.put(lotteryType, executor.submit(() -> runFetch(lotteryType.getCode(), fetchScope, task)));
            }

            for (Map.Entry<LotteryType, Future<Map<String, Object>>> entry : futures.entrySet()) {
                LotteryType lotteryType = entry.getKey();
                try {
                    Map<String, Object> result = entry.getValue().get();
                    summary.put(lotteryType.getCode(), result);
                    totalFetched += asInt(result.get("totalFetched"));
                    inserted += asInt(result.get("inserted"));
                    updated += asInt(result.get("updated"));
                    hasError = hasError || "failed".equals(result.get("status"));
                } catch (Exception e) {
                    hasError = true;
                    Map<String, Object> errorResult = buildErrorResult(lotteryType.getCode(), fetchScope.getScope(), e.getMessage());
                    summary.put(lotteryType.getCode(), errorResult);
                    if (task != null) {
                        task.recordTypeResult(lotteryType.getCode(), errorResult);
                    }
                    log.error("拉取 [{}] 异常: {}", lotteryType.getCode(), e.getMessage(), e);
                }
            }
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        summary.put("status", hasError ? "partial_failed" : "success");
        summary.put("totalFetched", totalFetched);
        summary.put("inserted", inserted);
        summary.put("updated", updated);
        summary.put("total", totalFetched);
        summary.put("new", inserted);
        return summary;
    }

    private Map<String, Object> runFetch(String lotteryType, FetchScope scope, FetchTask task) {
        log.info("开始拉取 [{}] 真实数据, scope={}", lotteryType, scope.getScope());
        if (task != null) {
            task.updateProgress(lotteryType, 0, 0, 0, 0);
        }

        FetchProgressCallback progressCallback = (page, total, ins, upd) -> {
            if (task != null) task.updateProgress(lotteryType, page, total, ins, upd);
            if (task != null) taskManager.persist(task);
        };

        int[] stats;
        try {
            stats = switch (lotteryType) {
                case "dlt", "pl3", "pl5" -> zhcwJsonFetcher.fetch(
                        lotteryType, scope.getLimitCount(), scope.getCutoffDateString(), scope.isAll(), progressCallback);
                case "ssq", "fc3d", "qlc" -> zhcwHtmlFetcher.fetch(
                        lotteryType, scope.getLimitCount(), scope.getCutoffDateString(), scope.isAll(), progressCallback);
                default -> new int[]{0, 0, 0};
            };
        } catch (Exception e) {
            log.warn("[{}] 真实数据拉取失败: {}", lotteryType, e.getMessage());
            Map<String, Object> errorResult = buildErrorResult(lotteryType, scope.getScope(), e.getMessage());
            if (task != null) task.recordTypeResult(lotteryType, errorResult);
            return errorResult;
        }

        log.info("[{}] 真实数据完成: 获取 {} 条, 新增 {} 条, 更新 {} 条",
                lotteryType, stats[0], stats[1], stats[2]);

        Map<String, Object> result = buildFetchResult(lotteryType, scope.getScope(), stats[0], stats[1], stats[2]);
        if (task != null) {
            task.recordTypeResult(lotteryType, result);
        }
        return result;
    }

    // ===== Scope 解析 =====

    private FetchScope resolveScope(String scope, Integer count) {
        if (count != null && count > 0) {
            return new FetchScope("latest-" + count, count, null, false, count);
        }
        String normalized = (scope == null || scope.isBlank()) ? "latest-1" : scope.trim();
        return switch (normalized) {
            case "latest-1" -> new FetchScope(normalized, 1, null, false, null);
            case "latest-10" -> new FetchScope(normalized, 10, null, false, null);
            case "latest-50" -> new FetchScope(normalized, 50, null, false, null);
            case "latest-100" -> new FetchScope(normalized, 100, null, false, null);
            case "year-1" -> new FetchScope(normalized, null, LocalDate.now().minusYears(1).toString(), false, null);
            case "year-3" -> new FetchScope(normalized, null, LocalDate.now().minusYears(3).toString(), false, null);
            case "all" -> new FetchScope(normalized, null, null, true, null);
            default -> throw new IllegalArgumentException("不支持的 scope: " + normalized);
        };
    }

    // ===== 构建结果 =====

    private Map<String, Object> buildFetchResult(String lotteryType, String scope, int total, int ins, int upd) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", lotteryType);
        result.put("name", resolveLotteryName(lotteryType));
        result.put("scope", scope);
        result.put("status", "success");
        result.put("currentPage", 0);
        result.put("page", 0);
        result.put("totalFetched", total);
        result.put("total", total);
        result.put("inserted", ins);
        result.put("new", ins);
        result.put("updated", upd);
        return result;
    }

    private Map<String, Object> buildErrorResult(String lotteryType, String scope, String errorMessage) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", lotteryType);
        result.put("name", resolveLotteryName(lotteryType));
        result.put("scope", scope);
        result.put("status", "failed");
        result.put("currentPage", 0);
        result.put("page", 0);
        result.put("totalFetched", 0);
        result.put("total", 0);
        result.put("inserted", 0);
        result.put("new", 0);
        result.put("updated", 0);
        result.put("error", errorMessage);
        return result;
    }

    private String resolveLotteryName(String lotteryType) {
        try {
            return LotteryType.fromCode(lotteryType).getName();
        } catch (Exception e) {
            return lotteryType;
        }
    }

    // ===== 辅助 =====

    private int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record FetchScope(String scope, Integer limitCount, String cutoffDate, boolean all, Integer requestedCount) {

        public boolean reachedLimit(int currentSize) {
            return limitCount != null && currentSize >= limitCount;
        }

        public boolean isBeforeCutoff(String drawDate) {
            if (all || cutoffDate == null || drawDate == null || drawDate.isBlank()) {
                return false;
            }
            String normalized = drawDate.trim();
            normalized = normalized.length() >= 10 ? normalized.substring(0, 10) : normalized;
            return normalized.compareTo(cutoffDate) < 0;
        }

        public String getCutoffDateString() {
            return cutoffDate;
        }

        public boolean isAll() {
            return all;
        }

        public Integer getLimitCount() {
            return limitCount;
        }

        public String getScope() {
            return scope;
        }
    }
}
