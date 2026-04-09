package com.lottery.service;

import com.lottery.entity.FetchHistoryDetail;
import com.lottery.entity.FetchHistoryTask;
import com.lottery.mapper.FetchHistoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FetchHistoryService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FetchHistoryMapper mapper;

    public void saveTask(Map<String, Object> taskData) {
        FetchHistoryTask task = new FetchHistoryTask();
        task.setTaskId(stringValue(taskData.get("taskId")));
        task.setTriggerSource(stringValue(taskData.get("triggerSource")));
        task.setType(stringValue(taskData.get("type")));
        task.setScope(stringValue(taskData.get("scope")));
        task.setMode(stringValue(taskData.get("mode")));
        task.setStatus(stringValue(taskData.get("status")));
        task.setCurrentType(blankToNull(stringValue(taskData.get("currentType"))));
        task.setCurrentPage(asInt(taskData.get("currentPage")));
        task.setTotalFetched(asInt(taskData.get("totalFetched")));
        task.setInserted(asInt(taskData.get("inserted")));
        task.setUpdated(asInt(taskData.get("updated")));
        task.setCompletedTypes(asInt(taskData.get("completedTypes")));
        task.setTotalTypes(asInt(taskData.get("totalTypes")));
        task.setError(blankToNull(stringValue(taskData.get("error"))));
        task.setStartedAt(blankToNull(stringValue(taskData.get("startedAt"))));
        task.setFinishedAt(blankToNull(stringValue(taskData.get("finishedAt"))));
        String now = nowString();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        mapper.upsertTask(task);
    }

    /**
     * 查询所有未完成的任务（status = pending 或 running）
     */
    public List<Map<String, Object>> findUnfinishedTasks() {
        List<FetchHistoryTask> pending = mapper.listTasks("pending", null, null, 100, 0);
        List<FetchHistoryTask> running = mapper.listTasks("running", null, null, 100, 0);
        List<Map<String, Object>> result = new ArrayList<>();
        for (FetchHistoryTask task : pending) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("taskId", task.getTaskId());
            map.put("status", task.getStatus());
            result.add(map);
        }
        for (FetchHistoryTask task : running) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("taskId", task.getTaskId());
            map.put("status", task.getStatus());
            result.add(map);
        }
        return result;
    }

    /**
     * 标记任务为 failed
     */
    public void markTaskFailed(String taskId, String error) {
        FetchHistoryTask task = mapper.findTaskById(taskId);
        if (task == null) return;
        task.setStatus("failed");
        task.setError(error);
        task.setFinishedAt(nowString());
        task.setUpdatedAt(nowString());
        mapper.upsertTask(task);
    }

    public void saveDetail(String taskId, Map<String, Object> result, int sortOrder) {
        FetchHistoryDetail detail = new FetchHistoryDetail();
        detail.setTaskId(taskId);
        detail.setLotteryType(stringValue(result.get("type")));
        detail.setName(blankToNull(stringValue(result.get("name"))));
        detail.setScope(blankToNull(stringValue(result.get("scope"))));
        detail.setStatus(stringValue(result.get("status")));
        detail.setCurrentPage(asInt(result.get("currentPage")));
        detail.setTotalFetched(asInt(result.get("totalFetched")));
        detail.setInserted(asInt(result.get("inserted")));
        detail.setUpdated(asInt(result.get("updated")));
        detail.setError(blankToNull(stringValue(result.get("error"))));
        detail.setSortOrder(sortOrder);
        String now = nowString();
        detail.setCreatedAt(now);
        detail.setUpdatedAt(now);
        mapper.upsertDetail(detail);
    }

    public Map<String, Object> list(String status, String triggerSource, String type, int limit, int offset) {
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 200);
        int safeOffset = Math.max(offset, 0);
        List<FetchHistoryTask> tasks = mapper.listTasks(status, triggerSource, type, safeLimit, safeOffset);
        List<Map<String, Object>> data = new ArrayList<>();
        for (FetchHistoryTask task : tasks) {
            data.add(toTaskMap(task));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", data);
        result.put("total", mapper.countTasks(status, triggerSource, type));
        result.put("limit", safeLimit);
        result.put("offset", safeOffset);
        return result;
    }

    public Map<String, Object> detail(String taskId) {
        FetchHistoryTask task = mapper.findTaskById(taskId);
        if (task == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskId", taskId);
            result.put("status", "not_found");
            result.put("error", "任务不存在");
            return result;
        }
        List<FetchHistoryDetail> details = mapper.listDetailsByTaskId(taskId);
        Map<String, Object> result = toTaskMap(task);
        Map<String, Object> results = new LinkedHashMap<>();
        List<Map<String, Object>> detailRows = new ArrayList<>();
        for (FetchHistoryDetail detail : details) {
            Map<String, Object> row = toDetailMap(detail);
            results.put(detail.getLotteryType(), row);
            detailRows.add(row);
        }
        if (!results.isEmpty()) {
            result.put("results", results);
            result.put("detailRows", detailRows);
        }
        return result;
    }

    private Map<String, Object> toTaskMap(FetchHistoryTask task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("taskId", task.getTaskId());
        map.put("triggerSource", task.getTriggerSource());
        map.put("type", task.getType());
        map.put("scope", task.getScope());
        map.put("mode", task.getMode());
        map.put("status", task.getStatus());
        map.put("currentType", task.getCurrentType());
        map.put("currentPage", defaultInt(task.getCurrentPage()));
        map.put("page", defaultInt(task.getCurrentPage()));
        map.put("totalFetched", defaultInt(task.getTotalFetched()));
        map.put("total", defaultInt(task.getTotalFetched()));
        map.put("inserted", defaultInt(task.getInserted()));
        map.put("new", defaultInt(task.getInserted()));
        map.put("updated", defaultInt(task.getUpdated()));
        map.put("completedTypes", defaultInt(task.getCompletedTypes()));
        map.put("totalTypes", defaultInt(task.getTotalTypes()));
        if (task.getStartedAt() != null && !task.getStartedAt().isBlank()) {
            map.put("startedAt", task.getStartedAt());
        }
        if (task.getFinishedAt() != null && !task.getFinishedAt().isBlank()) {
            map.put("finishedAt", task.getFinishedAt());
        }
        if (task.getError() != null && !task.getError().isBlank()) {
            map.put("error", task.getError());
        }
        return map;
    }

    private Map<String, Object> toDetailMap(FetchHistoryDetail detail) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", detail.getLotteryType());
        map.put("name", detail.getName());
        map.put("scope", detail.getScope());
        map.put("status", detail.getStatus());
        map.put("currentPage", defaultInt(detail.getCurrentPage()));
        map.put("page", defaultInt(detail.getCurrentPage()));
        map.put("totalFetched", defaultInt(detail.getTotalFetched()));
        map.put("total", defaultInt(detail.getTotalFetched()));
        map.put("inserted", defaultInt(detail.getInserted()));
        map.put("new", defaultInt(detail.getInserted()));
        map.put("updated", defaultInt(detail.getUpdated()));
        if (detail.getError() != null && !detail.getError().isBlank()) {
            map.put("error", detail.getError());
        }
        return map;
    }

    private int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return 0;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String nowString() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }
}
