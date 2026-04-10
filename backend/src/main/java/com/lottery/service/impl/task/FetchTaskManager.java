package com.lottery.service.impl.task;

import com.lottery.service.FetchHistoryService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 抓取任务生命周期管理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FetchTaskManager {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FetchHistoryService fetchHistoryService;
    private final Map<String, FetchTask> taskStore = new ConcurrentHashMap<>();

    public FetchTask createTask(String type, String scope, String mode, String triggerSource, int totalTypes) {
        FetchTask task = new FetchTask(type, scope, mode, triggerSource);
        task.setTotalTypes(totalTypes);
        taskStore.put(task.getTaskId(), task);
        persist(task);
        return task;
    }

    public FetchTask getTask(String taskId) {
        return taskStore.get(taskId);
    }

    /**
     * 启动时恢复：将未完成的任务标记为 failed
     */
    public void recoverUnfinishedTasks() {
        try {
            List<Map<String, Object>> unfinished = fetchHistoryService.findUnfinishedTasks();
            if (!unfinished.isEmpty()) {
                log.info("[启动恢复] 发现 {} 个未完成任务，标记为 failed", unfinished.size());
                for (Map<String, Object> taskData : unfinished) {
                    String taskId = String.valueOf(taskData.get("taskId"));
                    fetchHistoryService.markTaskFailed(taskId, "服务重启，任务中断");
                }
            }
        } catch (Exception e) {
            log.warn("[启动恢复] 恢复未完成任务失败: {}", e.getMessage());
        }
    }

    public void persist(FetchTask task) {
        fetchHistoryService.saveTask(task.toMap());
        int sortOrder = 0;
        for (Map.Entry<String, Object> entry : task.getResultsSnapshot().entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> resultMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cast = (Map<String, Object>) resultMap;
                fetchHistoryService.saveDetail(task.getTaskId(), cast, sortOrder++);
            }
        }
    }

    // ===== FetchTask 实体 =====

    @Getter
    public static class FetchTask {
        private final String taskId = UUID.randomUUID().toString();
        private final String type;
        private final String scope;
        private final String mode;
        private final String triggerSource;
        private final Map<String, TypeProgress> progressByType = new LinkedHashMap<>();
        private final Map<String, Object> results = new LinkedHashMap<>();

        private String status = "pending";
        private String currentType;
        private int currentPage;
        private int totalFetched;
        private int inserted;
        private int updated;
        private int totalTypes = 1;
        private int completedTypes;
        private String error;
        private String startedAt;
        private String finishedAt;

        FetchTask(String type, String scope, String mode, String triggerSource) {
            this.type = type;
            this.scope = scope;
            this.mode = mode;
            this.triggerSource = triggerSource;
        }

        public synchronized void setTotalTypes(int totalTypes) {
            this.totalTypes = totalTypes;
        }

        public synchronized Map<String, Object> getResultsSnapshot() {
            return new LinkedHashMap<>(results);
        }

        public synchronized void markRunning(String currentType) {
            this.status = "running";
            this.currentType = currentType;
            this.startedAt = now();
        }

        public synchronized void updateProgress(String lotteryType, int page, int total, int ins, int upd) {
            TypeProgress p = progressByType.computeIfAbsent(lotteryType, k -> new TypeProgress());
            p.currentPage = page;
            p.totalFetched = total;
            p.inserted = ins;
            p.updated = upd;
            currentType = lotteryType;
            currentPage = page;
            recalcTotals();
        }

        public synchronized void recordTypeResult(String lotteryType, Map<String, Object> result) {
            results.put(lotteryType, new LinkedHashMap<>(result));
            TypeProgress p = progressByType.computeIfAbsent(lotteryType, k -> new TypeProgress());
            p.currentPage = asInt(result.get("currentPage"));
            p.totalFetched = asInt(result.get("totalFetched"));
            p.inserted = asInt(result.get("inserted"));
            p.updated = asInt(result.get("updated"));
            p.error = strVal(result.get("error"));
            completedTypes = results.size();
            currentType = lotteryType;
            currentPage = p.currentPage;
            recalcTotals();
            if ("failed".equals(result.get("status")) && (error == null || error.isBlank())) {
                error = strVal(result.get("error"));
            }
        }

        public synchronized void markCompleted(String status, Map<String, Object> summary) {
            this.status = (status == null || status.isBlank()) ? "success" : status;
            this.finishedAt = now();
            if (totalTypes == 1 && results.isEmpty()) {
                results.put(type, new LinkedHashMap<>(summary));
                completedTypes = 1;
            }
            if ((error == null || error.isBlank()) && summary.get("error") != null) {
                error = strVal(summary.get("error"));
            }
        }

        public synchronized void markFailed(String error) {
            this.status = "failed";
            this.error = error;
            this.finishedAt = now();
        }

        public synchronized Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("taskId", taskId);
            map.put("type", type);
            map.put("scope", scope);
            map.put("mode", mode);
            map.put("triggerSource", triggerSource);
            map.put("status", status);
            map.put("currentType", currentType);
            map.put("currentPage", currentPage);
            map.put("page", currentPage);
            map.put("totalFetched", totalFetched);
            map.put("total", totalFetched);
            map.put("inserted", inserted);
            map.put("new", inserted);
            map.put("updated", updated);
            map.put("completedTypes", completedTypes);
            map.put("totalTypes", totalTypes);
            if (startedAt != null) map.put("startedAt", startedAt);
            if (finishedAt != null) map.put("finishedAt", finishedAt);
            if (error != null && !error.isBlank()) map.put("error", error);
            if (!results.isEmpty()) map.put("results", new LinkedHashMap<>(results));
            return map;
        }

        private void recalcTotals() {
            int f = 0, i = 0, u = 0;
            for (TypeProgress p : progressByType.values()) {
                f += p.totalFetched;
                i += p.inserted;
                u += p.updated;
            }
            totalFetched = f;
            inserted = i;
            updated = u;
        }

        private static int asInt(Object v) {
            if (v instanceof Number n) return n.intValue();
            if (v == null) return 0;
            return Integer.parseInt(String.valueOf(v));
        }

        private static String strVal(Object v) { return v == null ? "" : String.valueOf(v); }

        private static String now() { return LocalDateTime.now().format(TIME_FMT); }
    }

    static class TypeProgress {
        int currentPage;
        int totalFetched;
        int inserted;
        int updated;
        String error;
    }
}
