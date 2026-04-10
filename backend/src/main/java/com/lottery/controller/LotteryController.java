package com.lottery.controller;

import com.lottery.common.BusinessException;
import com.lottery.entity.LotteryType;
import com.lottery.service.AnalysisService;
import com.lottery.service.FetchService;
import com.lottery.service.LotteryResultService;
import com.lottery.service.RecommendationHistoryService;
import com.lottery.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/lottery")
@RequiredArgsConstructor
public class LotteryController {

    private final LotteryResultService resultService;
    private final FetchService fetchService;
    private final AnalysisService analysisService;
    private final RecommendationService recommendationService;
    private final RecommendationHistoryService recommendationHistoryService;

    // ===== 数据查询 =====

    @GetMapping("/results")
    public Map<String, Object> results(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        if (type != null) {
            validateType(type);
        }
        return Map.of(
                "data", resultService.queryReal(type, limit, offset),
                "total", resultService.countReal(type)
        );
    }

    @GetMapping("/latest")
    public Map<String, Object> latest(@RequestParam String type) {
        validateType(type);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("latestDrawNum", resultService.latestRealDrawNum(type));
        return result;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        var types = LotteryType.values();
        Map<String, Object> map = new LinkedHashMap<>();
        for (var t : types) {
            var latestResult = resultService.latestRealResult(t.getCode());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", t.getName());
            item.put("count", resultService.countReal(t.getCode()));
            item.put("latestDraw", latestResult == null ? null : latestResult.getDrawNum());
            item.put("latestNumbers", latestResult == null ? null : latestResult.getNumbers());
            map.put(t.getCode(), item);
        }
        return map;
    }

    // ===== 数据拉取 =====

    @PostMapping("/fetch")
    public Map<String, Object> fetchAll(
            @RequestParam(defaultValue = "latest-1") String scope,
            @RequestParam(required = false) Integer count) {
        return fetchService.startFetchAllTask(scope, count);
    }

    @PostMapping("/fetch/{type}")
    public Map<String, Object> fetchOne(
            @PathVariable String type,
            @RequestParam(defaultValue = "latest-1") String scope,
            @RequestParam(required = false) Integer count) {
        validateType(type);
        return fetchService.startFetchTask(type, scope, count);
    }

    @GetMapping("/fetch/tasks/{taskId}")
    public Map<String, Object> fetchTask(@PathVariable String taskId) {
        return fetchService.getFetchTask(taskId);
    }

    @GetMapping("/fetch/history")
    public Map<String, Object> fetchHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String triggerSource,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return fetchService.listFetchHistory(status, triggerSource, type, limit, offset);
    }

    @GetMapping("/fetch/history/{taskId}")
    public Map<String, Object> fetchHistoryDetail(@PathVariable String taskId) {
        return fetchService.getFetchHistory(taskId);
    }

    // ===== 统计分析 =====

    @GetMapping("/analyze")
    public Object analyze(@RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            validateType(type);
            return analysisService.analyze(type);
        }
        return analysisService.analyzeAll();
    }

    @GetMapping("/trend")
    public Map<String, Object> trend(
            @RequestParam String type,
            @RequestParam(defaultValue = "30") int n) {
        validateType(type);
        return analysisService.trend(type, n);
    }

    // ===== 号码推荐 =====

    @GetMapping("/recommend")
    public Map<String, Object> recommend(@RequestParam String type) {
        validateType(type);
        return recommendationService.recommend(type);
    }

    @GetMapping("/recommend/history")
    public Map<String, Object> recommendHistory(
            @RequestParam String type,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        validateType(type);
        return recommendationHistoryService.listHistory(type, limit, offset);
    }

    @GetMapping("/recommend/stats")
    public Map<String, Object> recommendStats(@RequestParam String type) {
        validateType(type);
        recommendationHistoryService.autoMatch(type);
        return recommendationHistoryService.getStats(type);
    }

    // ===== 内部方法 =====

    private void validateType(String type) {
        if (type == null || type.isBlank()) {
            throw BusinessException.badRequest("彩种参数不能为空");
        }
        try {
            LotteryType.fromCode(type);
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("不支持的彩种: " + type + "，可选值: ssq/dlt/fc3d/pl3/pl5/qlc");
        }
    }
}
