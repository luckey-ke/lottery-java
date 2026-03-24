package com.lottery.controller;

import com.lottery.service.AnalysisService;
import com.lottery.service.FetchService;
import com.lottery.service.LotteryResultService;
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

    /** 查询开奖记录 */
    @GetMapping("/results")
    public Map<String, Object> results(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return Map.of(
                "data", resultService.queryReal(type, limit, offset),
                "total", resultService.countReal(type)
        );
    }

    /** 获取某彩种最新期号 */
    @GetMapping("/latest")
    public Map<String, Object> latest(@RequestParam String type) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("latestDrawNum", resultService.latestRealDrawNum(type));
        return result;
    }

    /** 数据统计 */
    @GetMapping("/status")
    public Map<String, Object> status() {
        var types = com.lottery.entity.LotteryType.values();
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

    /** 手动拉取全部真实数据 */
    @PostMapping("/fetch")
    public Map<String, Object> fetchAll(
            @RequestParam(defaultValue = "latest-1") String scope,
            @RequestParam(required = false) Integer count) {
        return fetchService.startFetchAllTask(scope, count);
    }

    /** 手动拉取指定彩种真实数据 */
    @PostMapping("/fetch/{type}")
    public Map<String, Object> fetchOne(
            @PathVariable String type,
            @RequestParam(defaultValue = "latest-1") String scope,
            @RequestParam(required = false) Integer count) {
        return fetchService.startFetchTask(type, scope, count);
    }

    /** 查询抓取任务进度 */
    @GetMapping("/fetch/tasks/{taskId}")
    public Map<String, Object> fetchTask(@PathVariable String taskId) {
        return fetchService.getFetchTask(taskId);
    }

    /** 查询抓取历史 */
    @GetMapping("/fetch/history")
    public Map<String, Object> fetchHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String triggerSource,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return fetchService.listFetchHistory(status, triggerSource, type, limit, offset);
    }

    /** 查询抓取历史详情 */
    @GetMapping("/fetch/history/{taskId}")
    public Map<String, Object> fetchHistoryDetail(@PathVariable String taskId) {
        return fetchService.getFetchHistory(taskId);
    }

    /** 生成全部演示数据 */
    @PostMapping("/demo/fetch")
    public Map<String, Object> fetchAllDemo(
            @RequestParam(defaultValue = "100") int count) {
        return fetchService.fetchAllDemo(count);
    }

    /** 生成指定彩种演示数据 */
    @PostMapping("/demo/fetch/{type}")
    public Map<String, Object> fetchOneDemo(
            @PathVariable String type,
            @RequestParam(defaultValue = "100") int count) {
        return fetchService.fetchDemo(type, count);
    }

    /** 频率统计分析 */
    @GetMapping("/analyze")
    public Object analyze(@RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return analysisService.analyze(type);
        }
        return analysisService.analyzeAll();
    }

    /** 趋势分析 */
    @GetMapping("/trend")
    public Map<String, Object> trend(
            @RequestParam String type,
            @RequestParam(defaultValue = "30") int n) {
        return analysisService.trend(type, n);
    }
}
