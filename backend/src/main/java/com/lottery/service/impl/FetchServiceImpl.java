package com.lottery.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.entity.LotteryResult;
import com.lottery.entity.LotteryType;
import com.lottery.service.FetchHistoryService;
import com.lottery.service.FetchService;
import com.lottery.service.LotteryResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FetchServiceImpl implements FetchService {

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String SPORTTERY_BASE = "https://webapi.sporttery.cn/gateway/lottery/getHistoryPageListV1.qry";
    private static final int PAGE_SIZE = 200;
    private static final int FETCH_BATCH_SIZE = 200;
    private static final int DEFAULT_DEMO_COUNT = 100;
    private static final long ZHCW_PAGE_DELAY_MS = 1200;
    private static final long ZHCW_RETRY_DELAY_MS = 2000;
    private static final int ZHCW_MAX_EMPTY_RETRIES = 3;
    private static final int FETCH_ALL_THREAD_COUNT = 3;
    private static final int TASK_THREAD_COUNT = 4;
    private static final DateTimeFormatter TASK_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Pattern ZHCW_ROW_PATTERN = Pattern.compile(
            "(?s)<tr>\\s*<td align=\"center\">(\\d{4}-\\d{2}-\\d{2})</td>\\s*<td align=\"center\">(\\d+)</td>\\s*<td[^>]*>(.*?)</td>");
    private static final Pattern EM_NUMBER_PATTERN = Pattern.compile("<em[^>]*>(\\d+)</em>");
    private static final Pattern PAGE_NUM_PATTERN = Pattern.compile("pageNum=(\\d+)");

    private final LotteryResultService resultService;
    private final FetchHistoryService fetchHistoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(TASK_THREAD_COUNT);
    private final Map<String, FetchTask> taskStore = new ConcurrentHashMap<>();

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
        return startFetchAllTask(scope, count, "manual");
    }

    private Map<String, Object> startFetchAllTask(String scope, Integer count, String triggerSource) {
        FetchScope fetchScope = resolveScope(scope, count);
        FetchTask task = new FetchTask("all", fetchScope.getScope(), "concurrent-by-type", triggerSource);
        task.setTotalTypes(LotteryType.values().length);
        taskStore.put(task.getTaskId(), task);
        persistTask(task);
        taskExecutor.submit(() -> executeFetchAllTask(task, fetchScope));
        return task.toMap();
    }

    @Override
    public Map<String, Object> startFetchTask(String lotteryType, String scope, Integer count) {
        return startFetchTask(lotteryType, scope, count, "manual");
    }

    private Map<String, Object> startFetchTask(String lotteryType, String scope, Integer count, String triggerSource) {
        FetchScope fetchScope = resolveScope(scope, count);
        FetchTask task = new FetchTask(lotteryType, fetchScope.getScope(), "single", triggerSource);
        task.setTotalTypes(1);
        taskStore.put(task.getTaskId(), task);
        persistTask(task);
        taskExecutor.submit(() -> executeFetchSingleTask(task, lotteryType, fetchScope));
        return task.toMap();
    }

    @Override
    public Map<String, Object> getFetchTask(String taskId) {
        FetchTask task = taskStore.get(taskId);
        if (task == null) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskId", taskId);
            result.put("status", "not_found");
            result.put("error", "任务不存在");
            return result;
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
    public Map<String, Object> fetchAllDemo(int count) {
        int demoCount = count > 0 ? count : DEFAULT_DEMO_COUNT;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("count", demoCount);

        int totalFetched = 0;
        int inserted = 0;
        int updated = 0;
        boolean hasError = false;

        for (LotteryType lotteryType : LotteryType.values()) {
            try {
                Map<String, Object> result = fetchDemo(lotteryType.getCode(), demoCount);
                summary.put(lotteryType.getCode(), result);
                totalFetched += asInt(result.get("totalFetched"));
                inserted += asInt(result.get("inserted"));
                updated += asInt(result.get("updated"));
                hasError = hasError || "failed".equals(result.get("status"));
            } catch (Exception e) {
                hasError = true;
                summary.put(lotteryType.getCode(), buildErrorResult(lotteryType.getCode(), "demo", e.getMessage()));
                log.error("生成 demo [{}] 异常: {}", lotteryType.getCode(), e.getMessage(), e);
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

    @Override
    public Map<String, Object> fetchDemo(String lotteryType, int count) {
        int demoCount = count > 0 ? count : DEFAULT_DEMO_COUNT;
        FetchStats stats = new FetchStats();
        stats.setCurrentPage(1);

        try {
            List<LotteryResult> results = generateDemo(lotteryType, demoCount);
            for (LotteryResult result : results) {
                stats.increaseTotal();
                stats.recordSaveOutcome(resultService.saveDemo(result));
            }
            log.info("[{}] 演示数据完成: 生成 {} 条, 新增 {} 条, 更新 {} 条", lotteryType, results.size(), stats.getInsertedCount(), stats.getUpdatedCount());
            return buildFetchResult(lotteryType, "demo", stats);
        } catch (Exception e) {
            log.warn("[{}] 演示数据生成失败: {}", lotteryType, e.getMessage(), e);
            return buildFetchResult(lotteryType, "demo", FetchStats.failed(e.getMessage()));
        }
    }

    @Override
    public void fetchLatest() {
        startFetchAllTask("latest-1", 1, "scheduled");
    }

    private void executeFetchSingleTask(FetchTask task, String lotteryType, FetchScope scope) {
        try {
            task.markRunning(lotteryType);
            persistTask(task);
            Map<String, Object> result = runFetch(lotteryType, scope, task);
            task.markCompleted(stringValue(result.get("status")), result);
            persistTask(task);
        } catch (Exception e) {
            log.error("抓取任务 [{}] 执行失败: {}", task.getTaskId(), e.getMessage(), e);
            task.markFailed(e.getMessage());
            persistTask(task);
        }
    }

    private void executeFetchAllTask(FetchTask task, FetchScope scope) {
        try {
            task.markRunning("all");
            persistTask(task);
            Map<String, Object> result = runFetchAll(scope, task);
            task.markCompleted(stringValue(result.get("status")), result);
            persistTask(task);
        } catch (Exception e) {
            log.error("抓取全部任务 [{}] 执行失败: {}", task.getTaskId(), e.getMessage(), e);
            task.markFailed(e.getMessage());
            persistTask(task);
        }
    }

    private Map<String, Object> runFetchAll(FetchScope fetchScope, FetchTask task) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("scope", fetchScope.getScope());
        summary.put("mode", "concurrent-by-type");

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(FETCH_ALL_THREAD_COUNT, LotteryType.values().length));
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
            task.updateProgress(lotteryType, new FetchStats());
        }

        FetchStats stats = switch (lotteryType) {
            case "dlt", "pl3", "pl5" -> fetchFromSporttery(lotteryType, scope, task);
            case "ssq", "fc3d", "qlc" -> fetchFromZhcwHtml(lotteryType, scope, task);
            default -> FetchStats.failed("不支持的彩种: " + lotteryType);
        };

        if (stats.hasError()) {
            log.warn("[{}] 真实数据拉取失败: {}", lotteryType, stats.getError());
        } else {
            log.info("[{}] 真实数据完成: 获取 {} 条, 新增 {} 条, 更新 {} 条", lotteryType, stats.getTotal(), stats.getInsertedCount(), stats.getUpdatedCount());
        }

        Map<String, Object> result = buildFetchResult(lotteryType, scope.getScope(), stats);
        if (task != null) {
            task.recordTypeResult(lotteryType, result);
        }
        return result;
    }

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
            case "year-1" -> new FetchScope(normalized, null, LocalDate.now().minusYears(1), false, null);
            case "year-3" -> new FetchScope(normalized, null, LocalDate.now().minusYears(3), false, null);
            case "all" -> new FetchScope(normalized, null, null, true, null);
            default -> throw new IllegalArgumentException("不支持的 scope: " + normalized);
        };
    }

    private FetchStats fetchFromSporttery(String type, FetchScope scope, FetchTask task) {
        try {
            String gameNo = switch (type) {
                case "dlt" -> "85";
                case "pl3" -> "35";
                case "pl5" -> "350133";
                default -> null;
            };
            if (gameNo == null) {
                return FetchStats.failed("未找到体彩 gameNo: " + type);
            }

            FetchStats stats = new FetchStats();
            List<LotteryResult> batch = new ArrayList<>();
            Set<String> seen = new TreeSet<>();
            int pageNo = 1;
            boolean stop = false;

            while (!stop) {
                stats.setCurrentPage(pageNo);
                notifyTaskProgress(task, type, stats);

                String url = SPORTTERY_BASE + "?gameNo=" + gameNo + "&provinceId=0&isVerify=1&pageNo=" + pageNo + "&pageSize=" + PAGE_SIZE;
                if (scope.getLimitCount() != null && pageNo == 1 && scope.getLimitCount() <= PAGE_SIZE) {
                    url += "&termLimits=" + scope.getLimitCount();
                }
                String body = sendGet(url, true);
                JsonNode root = objectMapper.readTree(body);
                JsonNode value = root.path("value");
                JsonNode items = value.path("list");
                if (!root.path("success").asBoolean(false) || !items.isArray() || items.isEmpty()) {
                    break;
                }

                int before = stats.getTotal();
                for (JsonNode item : items) {
                    LotteryResult result = toSportteryResult(type, item);
                    if (result == null) {
                        continue;
                    }
                    if (scope.isBeforeCutoff(result.getDrawDate())) {
                        stop = true;
                        break;
                    }
                    String key = result.getLotteryType() + "#" + result.getDrawNum();
                    if (!seen.add(key)) {
                        continue;
                    }
                    batch.add(result);
                    stats.increaseTotal();
                    if (batch.size() >= FETCH_BATCH_SIZE) {
                        flushBatch(batch, stats);
                        notifyTaskProgress(task, type, stats);
                    }
                    if (scope.reachedLimit(stats.getTotal())) {
                        stop = true;
                        break;
                    }
                }
                notifyTaskProgress(task, type, stats);
                if (stats.getTotal() == before) {
                    break;
                }

                int totalPages = value.path("pages").asInt(0);
                int pageSize = value.path("pageSize").asInt(PAGE_SIZE);
                if ((totalPages > 0 && pageNo >= totalPages) || items.size() < pageSize) {
                    break;
                }
                pageNo++;
            }

            flushBatch(batch, stats);
            notifyTaskProgress(task, type, stats);
            if (stats.getTotal() > 0) {
                log.info("体彩接口获取到 {} 条 [{}], scope={}", stats.getTotal(), type, scope.getScope());
            }
            return stats;
        } catch (Exception e) {
            log.warn("体彩接口请求失败 ({}): {}", type, e.getMessage(), e);
            return FetchStats.failed(e.getMessage());
        }
    }

    private LotteryResult toSportteryResult(String type, JsonNode item) {
        String drawNum = item.path("lotteryDrawNum").asText("");
        String drawDate = normalizeDate(item.path("lotteryDrawTime").asText(""));
        String drawResult = item.path("lotteryDrawResult").asText("");
        String numbers = formatSportteryNumbers(type, drawResult);
        if (drawNum.isBlank() || drawDate.isBlank() || numbers.isBlank()) {
            return null;
        }

        LotteryResult result = new LotteryResult();
        result.setLotteryType(type);
        result.setDrawNum(drawNum);
        result.setDrawDate(drawDate);
        result.setNumbers(numbers);
        return result;
    }

    private FetchStats fetchFromZhcwHtml(String type, FetchScope scope, FetchTask task) {
        try {
            String pageUrlPattern = switch (type) {
                case "ssq" -> "https://kaijiang.zhcw.com/zhcw/inc/ssq/ssq_wqhg.jsp?pageNum=%d";
                case "fc3d" -> "https://kaijiang.zhcw.com/zhcw/inc/3d/3d_wqhg.jsp?pageNum=%d";
                case "qlc" -> "https://kaijiang.zhcw.com/zhcw/inc/qlc/qlc_wqhg.jsp?pageNum=%d";
                default -> null;
            };
            if (pageUrlPattern == null) {
                return FetchStats.failed("未找到中彩网抓取地址: " + type);
            }

            FetchStats stats = new FetchStats();
            List<LotteryResult> batch = new ArrayList<>();
            Set<String> seen = new TreeSet<>();
            int pageNo = 1;
            int emptyRetryCount = 0;
            boolean stop = false;

            while (!stop) {
                stats.setCurrentPage(pageNo);
                notifyTaskProgress(task, type, stats);

                String html = sendGet(pageUrlPattern.formatted(pageNo), false);
                List<LotteryResult> pageResults = parseZhcwHtml(type, html);
                int lastPage = extractLastPageNo(html);
                if (pageResults.isEmpty()) {
                    if (pageNo <= lastPage && emptyRetryCount < ZHCW_MAX_EMPTY_RETRIES) {
                        emptyRetryCount++;
                        log.warn("中彩网第 {} 页 [{}] 返回空数据，准备重试 {}/{}", pageNo, type, emptyRetryCount, ZHCW_MAX_EMPTY_RETRIES);
                        sleepQuietly(ZHCW_RETRY_DELAY_MS * emptyRetryCount);
                        continue;
                    }
                    break;
                }
                emptyRetryCount = 0;

                int before = stats.getTotal();
                for (LotteryResult result : pageResults) {
                    if (scope.isBeforeCutoff(result.getDrawDate())) {
                        stop = true;
                        break;
                    }
                    String key = result.getLotteryType() + "#" + result.getDrawNum();
                    if (!seen.add(key)) {
                        continue;
                    }
                    batch.add(result);
                    stats.increaseTotal();
                    if (batch.size() >= FETCH_BATCH_SIZE) {
                        flushBatch(batch, stats);
                        notifyTaskProgress(task, type, stats);
                    }
                    if (scope.reachedLimit(stats.getTotal())) {
                        stop = true;
                        break;
                    }
                }
                notifyTaskProgress(task, type, stats);
                if (stats.getTotal() == before || (lastPage > 0 && pageNo >= lastPage)) {
                    break;
                }
                pageNo++;
                sleepQuietly(ZHCW_PAGE_DELAY_MS);
            }

            flushBatch(batch, stats);
            notifyTaskProgress(task, type, stats);
            if (stats.getTotal() > 0) {
                log.info("中彩网页面获取到 {} 条 [{}], scope={}", stats.getTotal(), type, scope.getScope());
            }
            return stats;
        } catch (Exception e) {
            log.warn("中彩网页面请求失败 ({}) : {}", type, e.getMessage(), e);
            return FetchStats.failed(e.getMessage());
        }
    }

    private void notifyTaskProgress(FetchTask task, String lotteryType, FetchStats stats) {
        if (task != null) {
            task.updateProgress(lotteryType, stats);
            persistTask(task);
        }
    }

    private List<LotteryResult> parseZhcwHtml(String type, String html) {
        List<LotteryResult> list = new ArrayList<>();
        Matcher rowMatcher = ZHCW_ROW_PATTERN.matcher(html);
        while (rowMatcher.find()) {
            String drawDate = rowMatcher.group(1);
            String drawNum = rowMatcher.group(2);
            String numbersHtml = rowMatcher.group(3);
            List<String> nums = extractEmNumbers(numbersHtml);
            String numbers = formatZhcwNumbers(type, nums);
            if (numbers.isBlank()) {
                continue;
            }

            LotteryResult result = new LotteryResult();
            result.setLotteryType(type);
            result.setDrawNum(drawNum);
            result.setDrawDate(drawDate);
            result.setNumbers(numbers);
            list.add(result);
        }
        return list;
    }

    private List<String> extractEmNumbers(String html) {
        List<String> nums = new ArrayList<>();
        Matcher matcher = EM_NUMBER_PATTERN.matcher(html);
        while (matcher.find()) {
            nums.add(matcher.group(1));
        }
        return nums;
    }

    private String formatZhcwNumbers(String type, List<String> nums) {
        return switch (type) {
            case "ssq" -> nums.size() >= 7
                    ? joinWithComma(nums.subList(0, 6)) + "+" + pad2(nums.get(6))
                    : "";
            case "fc3d" -> nums.size() >= 3
                    ? String.join(",", nums.subList(0, 3))
                    : "";
            case "qlc" -> nums.size() >= 8
                    ? joinWithComma(nums.subList(0, 7)) + "+" + pad2(nums.get(7))
                    : nums.size() >= 7
                    ? joinWithComma(nums.subList(0, 7))
                    : "";
            default -> "";
        };
    }

    private String formatSportteryNumbers(String type, String drawResult) {
        String[] arr = drawResult.trim().split("\\s+");
        return switch (type) {
            case "dlt" -> arr.length >= 7
                    ? joinWithComma(List.of(arr[0], arr[1], arr[2], arr[3], arr[4])) + "+" + joinWithComma(List.of(arr[5], arr[6]))
                    : "";
            case "pl3" -> arr.length >= 3
                    ? String.join(",", arr[0], arr[1], arr[2])
                    : "";
            case "pl5" -> arr.length >= 5
                    ? String.join(",", arr[0], arr[1], arr[2], arr[3], arr[4])
                    : "";
            default -> "";
        };
    }

    private String joinWithComma(List<String> nums) {
        List<String> padded = new ArrayList<>();
        for (String num : nums) {
            padded.add(pad2(num));
        }
        return String.join(",", padded);
    }

    private String pad2(String num) {
        int value = Integer.parseInt(num.trim());
        return String.format("%02d", value);
    }

    private String normalizeDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String trimmed = raw.trim();
        return trimmed.length() >= 10 ? trimmed.substring(0, 10) : trimmed;
    }

    private int extractLastPageNo(String html) {
        int lastPage = 0;
        Matcher matcher = PAGE_NUM_PATTERN.matcher(html);
        while (matcher.find()) {
            lastPage = Math.max(lastPage, Integer.parseInt(matcher.group(1)));
        }
        return lastPage;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待中断", e);
        }
    }

    private void flushBatch(List<LotteryResult> batch, FetchStats stats) {
        if (batch.isEmpty()) {
            return;
        }
        for (LotteryResult result : batch) {
            stats.recordSaveOutcome(resultService.saveReal(result));
        }
        batch.clear();
    }

    private String sendGet(String url, boolean expectJson) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", UA)
                .header("Accept", expectJson ? "application/json,text/plain,*/*" : "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .timeout(Duration.ofSeconds(20))
                .GET();

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    @Override
    public List<LotteryResult> generateDemo(String type, int count) {
        Random rnd = new Random();
        List<LotteryResult> list = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < count; i++) {
            LocalDate dateValue = today.minusDays(i);
            String date = dateValue.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String drawNum = dateValue.getYear() + String.format("%03d", count - i);

            LotteryResult result = new LotteryResult();
            result.setLotteryType(type);
            result.setDrawNum(drawNum);
            result.setDrawDate(date);

            switch (type) {
                case "ssq" -> {
                    List<Integer> reds = randomSorted(rnd, 34, 6);
                    int blue = rnd.nextInt(1, 17);
                    result.setNumbers(fmt(reds) + "+" + fmt2(blue));
                }
                case "dlt" -> {
                    List<Integer> fronts = randomSorted(rnd, 36, 5);
                    List<Integer> backs = randomSorted(rnd, 13, 2);
                    result.setNumbers(fmt(fronts) + "+" + fmt(backs));
                }
                case "fc3d", "pl3" -> result.setNumbers(rnd.nextInt(10) + "," + rnd.nextInt(10) + "," + rnd.nextInt(10));
                case "pl5" -> result.setNumbers(rnd.nextInt(10) + "," + rnd.nextInt(10) + "," + rnd.nextInt(10) +
                        "," + rnd.nextInt(10) + "," + rnd.nextInt(10));
                case "qlc" -> {
                    List<Integer> nums = randomSorted(rnd, 31, 7);
                    int special = randomSpecialExcluding(rnd, 31, nums);
                    result.setNumbers(fmt(nums) + "+" + fmt2(special));
                }
                default -> {
                    continue;
                }
            }
            list.add(result);
        }
        return list;
    }

    private List<Integer> randomSorted(Random rnd, int maxExclusive, int count) {
        Set<Integer> set = new TreeSet<>();
        while (set.size() < count) {
            set.add(rnd.nextInt(1, maxExclusive));
        }
        return new ArrayList<>(set);
    }

    private int randomSpecialExcluding(Random rnd, int maxExclusive, List<Integer> existing) {
        int value;
        do {
            value = rnd.nextInt(1, maxExclusive);
        } while (existing.contains(value));
        return value;
    }

    private String fmt(List<Integer> nums) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < nums.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(String.format("%02d", nums.get(i)));
        }
        return sb.toString();
    }

    private String fmt2(int n) {
        return String.format("%02d", n);
    }

    private Map<String, Object> buildFetchResult(String lotteryType, String scope, FetchStats stats) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", lotteryType);
        result.put("name", resolveLotteryName(lotteryType));
        result.put("scope", scope);
        result.put("status", stats.hasError() ? "failed" : "success");
        result.put("currentPage", stats.getCurrentPage());
        result.put("page", stats.getCurrentPage());
        result.put("totalFetched", stats.getTotal());
        result.put("total", stats.getTotal());
        result.put("inserted", stats.getInsertedCount());
        result.put("new", stats.getInsertedCount());
        result.put("updated", stats.getUpdatedCount());
        if (stats.hasError()) {
            result.put("error", stats.getError());
        }
        return result;
    }

    private Map<String, Object> buildErrorResult(String lotteryType, String scope, String errorMessage) {
        return buildFetchResult(lotteryType, scope, FetchStats.failed(errorMessage));
    }

    private String resolveLotteryName(String lotteryType) {
        try {
            return LotteryType.fromCode(lotteryType).getName();
        } catch (Exception e) {
            return lotteryType;
        }
    }

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

    private String nowString() {
        return LocalDateTime.now().format(TASK_TIME_FORMATTER);
    }

    private void persistTask(FetchTask task) {
        Map<String, Object> taskData = task.toMap();
        fetchHistoryService.saveTask(taskData);
        Map<String, Object> results = task.getResultsSnapshot();
        int sortOrder = 0;
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> resultMap) {
                fetchHistoryService.saveDetail(task.getTaskId(), castMap(resultMap), sortOrder++);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> source) {
        return (Map<String, Object>) source;
    }

    private static final class FetchStats {
        private int total;
        private int insertedCount;
        private int updatedCount;
        private int currentPage;
        private String error;

        static FetchStats failed(String error) {
            FetchStats stats = new FetchStats();
            stats.error = error;
            return stats;
        }

        public int getTotal() {
            return total;
        }

        public int getInsertedCount() {
            return insertedCount;
        }

        public int getUpdatedCount() {
            return updatedCount;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public String getError() {
            return error;
        }

        public boolean hasError() {
            return error != null && !error.isBlank();
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }

        public void increaseTotal() {
            total++;
        }

        public void recordSaveOutcome(LotteryResultService.SaveOutcome outcome) {
            if (outcome == LotteryResultService.SaveOutcome.INSERTED) {
                insertedCount++;
            } else if (outcome == LotteryResultService.SaveOutcome.UPDATED) {
                updatedCount++;
            }
        }
    }

    private static final class FetchScope {
        private final String scope;
        private final Integer limitCount;
        private final LocalDate cutoffDate;
        private final boolean all;
        private final Integer requestedCount;

        private FetchScope(String scope, Integer limitCount, LocalDate cutoffDate, boolean all, Integer requestedCount) {
            this.scope = scope;
            this.limitCount = limitCount;
            this.cutoffDate = cutoffDate;
            this.all = all;
            this.requestedCount = requestedCount;
        }

        public String getScope() {
            return scope;
        }

        public Integer getLimitCount() {
            return limitCount;
        }

        public Integer getRequestedCount() {
            return requestedCount;
        }

        public boolean reachedLimit(int currentSize) {
            return limitCount != null && currentSize >= limitCount;
        }

        public boolean isBeforeCutoff(String drawDate) {
            if (all || cutoffDate == null || drawDate == null || drawDate.isBlank()) {
                return false;
            }
            LocalDate currentDate = LocalDate.parse(normalizeDateStatic(drawDate), DateTimeFormatter.ISO_LOCAL_DATE);
            return currentDate.isBefore(cutoffDate);
        }

        private static String normalizeDateStatic(String raw) {
            String trimmed = raw.trim();
            return trimmed.length() >= 10 ? trimmed.substring(0, 10) : trimmed;
        }
    }

    private final class FetchTask {
        private final String taskId;
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
        private Map<String, Object> summary;

        private FetchTask(String type, String scope, String mode, String triggerSource) {
            this.taskId = UUID.randomUUID().toString();
            this.type = type;
            this.scope = scope;
            this.mode = mode;
            this.triggerSource = triggerSource;
        }

        public String getTaskId() {
            return taskId;
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
            this.startedAt = nowString();
        }

        public synchronized void updateProgress(String lotteryType, FetchStats stats) {
            TypeProgress progress = progressByType.computeIfAbsent(lotteryType, key -> new TypeProgress());
            progress.currentPage = stats.getCurrentPage();
            progress.totalFetched = stats.getTotal();
            progress.inserted = stats.getInsertedCount();
            progress.updated = stats.getUpdatedCount();
            progress.error = stats.getError();
            currentType = lotteryType;
            currentPage = stats.getCurrentPage();
            recalculateTotals();
        }

        public synchronized void recordTypeResult(String lotteryType, Map<String, Object> result) {
            results.put(lotteryType, new LinkedHashMap<>(result));
            TypeProgress progress = progressByType.computeIfAbsent(lotteryType, key -> new TypeProgress());
            progress.currentPage = asInt(result.get("currentPage"));
            progress.totalFetched = asInt(result.get("totalFetched"));
            progress.inserted = asInt(result.get("inserted"));
            progress.updated = asInt(result.get("updated"));
            progress.error = stringValue(result.get("error"));
            completedTypes = results.size();
            currentType = lotteryType;
            currentPage = progress.currentPage;
            recalculateTotals();
            if ("failed".equals(result.get("status")) && (error == null || error.isBlank())) {
                error = stringValue(result.get("error"));
            }
        }

        public synchronized void markCompleted(String status, Map<String, Object> summary) {
            this.status = status == null || status.isBlank() ? "success" : status;
            this.summary = new LinkedHashMap<>(summary);
            this.finishedAt = nowString();
            if (totalTypes == 1 && results.isEmpty()) {
                results.put(type, new LinkedHashMap<>(summary));
                completedTypes = 1;
            }
            if ((error == null || error.isBlank()) && summary.get("error") != null) {
                error = stringValue(summary.get("error"));
            }
        }

        public synchronized void markFailed(String error) {
            this.status = "failed";
            this.error = error;
            this.finishedAt = nowString();
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
            if (startedAt != null) {
                map.put("startedAt", startedAt);
            }
            if (finishedAt != null) {
                map.put("finishedAt", finishedAt);
            }
            if (error != null && !error.isBlank()) {
                map.put("error", error);
            }
            if (!results.isEmpty()) {
                map.put("results", new LinkedHashMap<>(results));
            }
            if (summary != null) {
                map.put("summary", new LinkedHashMap<>(summary));
            }
            return map;
        }

        private void recalculateTotals() {
            int fetchedSum = 0;
            int insertedSum = 0;
            int updatedSum = 0;
            for (TypeProgress progress : progressByType.values()) {
                fetchedSum += progress.totalFetched;
                insertedSum += progress.inserted;
                updatedSum += progress.updated;
            }
            totalFetched = fetchedSum;
            inserted = insertedSum;
            updated = updatedSum;
        }
    }

    private static final class TypeProgress {
        private int currentPage;
        private int totalFetched;
        private int inserted;
        private int updated;
        private String error;
    }
}
