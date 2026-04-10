package com.lottery.service.impl.fetcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.common.FetcherUtils;
import com.lottery.entity.LotteryResult;
import com.lottery.service.LotteryResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 中彩网 JSON 接口数据抓取器（含体彩网备用接口）
 * <p>适用彩种：大乐透(dlt)、排列三(pl3)、排列五(pl5)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZhcwJsonFetcher {

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";
    private static final String ZHCW_JSON_BASE = "https://jc.zhcw.com/port/client_json.php";
    private static final String SPORTTERY_BASE = "https://webapi.sporttery.cn/gateway/lottery/getHistoryPageListV1.qry";
    private static final int PAGE_SIZE = 200;
    private static final int FETCH_BATCH_SIZE = 200;
    private static final long PAGE_DELAY_MS = 600;
    private static final int MAX_PAGE_RETRIES = 3;
    private static final long PAGE_RETRY_BASE_DELAY_MS = 3000;

    private static final String EXTRA_KEY_SALES_AMOUNT = "salesAmount";
    private static final String EXTRA_KEY_FIRST_PRIZE = "firstPrize";
    private static final String EXTRA_KEY_SECOND_PRIZE = "secondPrize";
    private static final String EXTRA_KEY_DETAIL = "detail";
    private static final String DETAIL_SEPARATOR = "；";

    private final LotteryResultService resultService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * 抓取指定彩种的开奖数据
     */
    public int[] fetch(String type, Integer limitCount, String cutoffDate, boolean fetchAll,
                       FetchProgressCallback progressCallback) {
        int[] result = fetchFromZhcwJson(type, limitCount, cutoffDate, fetchAll, progressCallback);

        // 如果中彩网 JSON 接口失败或无数据，尝试体彩网接口
        if (result[0] == 0 && (type.equals("dlt") || type.equals("pl3") || type.equals("pl5"))) {
            log.info("[{}] 中彩网JSON无数据，尝试体彩网接口", type);
            result = fetchFromSporttery(type, limitCount, cutoffDate, fetchAll, progressCallback);
        }

        return result;
    }

    // ===== 中彩网 JSON 接口 =====

    private int[] fetchFromZhcwJson(String type, Integer limitCount, String cutoffDate, boolean fetchAll,
                                     FetchProgressCallback progressCallback) {
        String lotteryId = getLotteryId(type);
        if (lotteryId == null) return new int[]{0, 0, 0};

        AtomicInteger total = new AtomicInteger();
        AtomicInteger inserted = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();
        List<LotteryResult> batch = new ArrayList<>();
        Set<String> seen = new TreeSet<>();
        int pageNo = 1;
        int pageRetryCount = 0;
        String referer = "https://www.zhcw.com/kjxx/" + type + "/";
        boolean stop = false;

        while (!stop) {
            if (progressCallback != null) {
                progressCallback.onProgress(pageNo, total.get(), inserted.get(), updated.get());
            }

            try {
                long ts = System.currentTimeMillis() / 1000;
                String url = ZHCW_JSON_BASE
                        + "?transactionType=10001001&lotteryId=" + lotteryId
                        + "&type=0&pageNum=" + pageNo + "&pageSize=" + PAGE_SIZE
                        + "&issueCount=" + PAGE_SIZE
                        + "&startIssue=&endIssue=&startDate=&endDate="
                        + "&tt=" + ts + "&callback=cb";

                String body = sendGet(url, referer);
                if (body.startsWith("cb(") && body.endsWith(")")) {
                    body = body.substring(3, body.length() - 1);
                }

                JsonNode root = objectMapper.readTree(body);
                JsonNode items = resolveItems(root);

                if (!items.isArray() || items.isEmpty()) break;

                int before = total.get();
                for (JsonNode item : items) {
                    LotteryResult result = toResult(type, item);
                    if (result == null) continue;
                    if (!fetchAll && cutoffDate != null && result.getDrawDate().compareTo(cutoffDate) < 0) {
                        stop = true;
                        break;
                    }
                    String key = result.getLotteryType() + "#" + result.getDrawNum();
                    if (!seen.add(key)) continue;

                    batch.add(result);
                    total.incrementAndGet();
                    if (batch.size() >= FETCH_BATCH_SIZE) {
                        flushBatch(batch, inserted, updated);
                        if (progressCallback != null) {
                            progressCallback.onProgress(pageNo, total.get(), inserted.get(), updated.get());
                        }
                    }
                    if (limitCount != null && total.get() >= limitCount) {
                        stop = true;
                        break;
                    }
                }

                if (progressCallback != null) {
                    progressCallback.onProgress(pageNo, total.get(), inserted.get(), updated.get());
                }
                if (total.get() == before) break;
                pageRetryCount = 0;

                int totalPages = resolveTotalPages(root);
                if (totalPages > 0 && pageNo >= totalPages) break;
                pageNo++;
                sleepQuietly(PAGE_DELAY_MS);

            } catch (Exception e) {
                if (isRecoverable(e) && pageRetryCount < MAX_PAGE_RETRIES) {
                    pageRetryCount++;
                    long delay = PAGE_RETRY_BASE_DELAY_MS * (1L << (pageRetryCount - 1));
                    log.warn("中彩网JSON请求失败 ({}) 第 {}/{}, {}ms 后重试: {}",
                            type, pageRetryCount, MAX_PAGE_RETRIES, delay, e.getMessage());
                    sleepQuietly(delay);
                    continue;
                }
                log.warn("中彩网JSON请求失败 ({}): {}", type, e.getMessage());
                break;
            }
        }

        flushBatch(batch, inserted, updated);
        if (total.get() > 0) {
            log.info("中彩网JSON获取到 {} 条 [{}]", total.get(), type);
        }
        return new int[]{total.get(), inserted.get(), updated.get()};
    }

    // ===== 体彩网备用接口 =====

    private int[] fetchFromSporttery(String type, Integer limitCount, String cutoffDate, boolean fetchAll,
                                      FetchProgressCallback progressCallback) {
        String gameNo = switch (type) {
            case "dlt" -> "85";
            case "pl3" -> "35";
            case "pl5" -> "350133";
            default -> null;
        };
        if (gameNo == null) return new int[]{0, 0, 0};

        AtomicInteger total = new AtomicInteger();
        AtomicInteger inserted = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();
        List<LotteryResult> batch = new ArrayList<>();
        Set<String> seen = new TreeSet<>();
        int pageNo = 1;
        int pageRetryCount = 0;
        boolean stop = false;

        while (!stop) {
            if (progressCallback != null) {
                progressCallback.onProgress(pageNo, total.get(), inserted.get(), updated.get());
            }

            try {
                String url = SPORTTERY_BASE + "?gameNo=" + gameNo + "&provinceId=0&isVerify=1"
                        + "&pageNo=" + pageNo + "&pageSize=" + PAGE_SIZE;
                if (limitCount != null && pageNo == 1 && limitCount <= PAGE_SIZE) {
                    url += "&termLimits=" + limitCount;
                }

                String body = sendGet(url, null);
                JsonNode root = objectMapper.readTree(body);
                JsonNode items = root.path("value").path("list");
                if (!root.path("success").asBoolean(false) || !items.isArray() || items.isEmpty()) break;

                int before = total.get();
                for (JsonNode item : items) {
                    LotteryResult result = toSportteryResult(type, item);
                    if (result == null) continue;
                    if (!fetchAll && cutoffDate != null && result.getDrawDate().compareTo(cutoffDate) < 0) {
                        stop = true;
                        break;
                    }
                    String key = result.getLotteryType() + "#" + result.getDrawNum();
                    if (!seen.add(key)) continue;

                    batch.add(result);
                    total.incrementAndGet();
                    if (batch.size() >= FETCH_BATCH_SIZE) {
                        flushBatch(batch, inserted, updated);
                        if (progressCallback != null) {
                            progressCallback.onProgress(pageNo, total.get(), inserted.get(), updated.get());
                        }
                    }
                    if (limitCount != null && total.get() >= limitCount) {
                        stop = true;
                        break;
                    }
                }

                if (progressCallback != null) {
                    progressCallback.onProgress(pageNo, total.get(), inserted.get(), updated.get());
                }
                if (total.get() == before) break;
                pageRetryCount = 0;

                int totalPages = root.path("value").path("pages").asInt(0);
                int pgSize = root.path("value").path("pageSize").asInt(PAGE_SIZE);
                if ((totalPages > 0 && pageNo >= totalPages) || items.size() < pgSize) break;
                pageNo++;

            } catch (Exception e) {
                if (isRecoverable(e) && pageRetryCount < MAX_PAGE_RETRIES) {
                    pageRetryCount++;
                    long delay = PAGE_RETRY_BASE_DELAY_MS * (1L << (pageRetryCount - 1));
                    log.warn("体彩接口请求失败 ({}) 第 {}/{}, {}ms 后重试: {}",
                            type, pageRetryCount, MAX_PAGE_RETRIES, delay, e.getMessage());
                    sleepQuietly(delay);
                    continue;
                }
                log.warn("体彩接口请求失败 ({}): {}", type, e.getMessage());
                break;
            }
        }

        flushBatch(batch, inserted, updated);
        if (total.get() > 0) log.info("体彩网获取到 {} 条 [{}]", total.get(), type);
        return new int[]{total.get(), inserted.get(), updated.get()};
    }

    // ===== 数据转换 =====

    private LotteryResult toResult(String type, JsonNode item) {
        String drawNum = textValue(item, "issue");
        String drawDate = normalizeDate(textValue(item, "openTime"));
        String frontWinningNum = textValue(item, "frontWinningNum");
        String backWinningNum = textValue(item, "backWinningNum");
        if (drawNum.isBlank() || drawDate.isBlank() || frontWinningNum.isBlank()) return null;

        String numbers = formatZhcwNumbers(type, frontWinningNum, backWinningNum);
        if (numbers.isBlank()) return null;

        LotteryResult result = new LotteryResult();
        result.setLotteryType(type);
        result.setDrawNum(drawNum);
        result.setDrawDate(drawDate);
        result.setNumbers(numbers);
        result.setExtraInfo(buildZhcwExtraInfo(item));
        return result;
    }

    private LotteryResult toSportteryResult(String type, JsonNode item) {
        String drawNum = item.path("lotteryDrawNum").asText("");
        String drawDate = normalizeDate(item.path("lotteryDrawTime").asText(""));
        String drawResult = item.path("lotteryDrawResult").asText("");
        if (drawNum.isBlank() || drawDate.isBlank() || drawResult.isBlank()) return null;

        LotteryResult result = new LotteryResult();
        result.setLotteryType(type);
        result.setDrawNum(drawNum);
        result.setDrawDate(drawDate);
        result.setNumbers(formatSportteryNumbers(type, drawResult));
        result.setExtraInfo(buildSportteryExtraInfo(item));
        return result;
    }

    private String formatZhcwNumbers(String type, String frontWinningNum, String backWinningNum) {
        String[] front = frontWinningNum.trim().split("\\s+");
        String back = (backWinningNum == null || backWinningNum.isBlank()) ? "" : backWinningNum.trim();
        String[] backArr = back.isEmpty() ? new String[0] : back.split("\\s+");

        return switch (type) {
            case "dlt" -> front.length >= 5 && backArr.length >= 2
                    ? pad2(front[0]) + "," + pad2(front[1]) + "," + pad2(front[2]) + ","
                    + pad2(front[3]) + "," + pad2(front[4]) + "+" + pad2(backArr[0]) + "," + pad2(backArr[1]) : "";
            case "pl3" -> front.length >= 3
                    ? front[0] + "," + front[1] + "," + front[2] : "";
            case "pl5" -> front.length >= 5
                    ? front[0] + "," + front[1] + "," + front[2] + "," + front[3] + "," + front[4] : "";
            default -> "";
        };
    }

    private String formatSportteryNumbers(String type, String drawResult) {
        String[] arr = drawResult.trim().split("\\s+");
        return switch (type) {
            case "dlt" -> arr.length >= 7
                    ? joinComma(arr[0], arr[1], arr[2], arr[3], arr[4]) + "+" + joinComma(arr[5], arr[6]) : "";
            case "pl3" -> arr.length >= 3 ? String.join(",", arr[0], arr[1], arr[2]) : "";
            case "pl5" -> arr.length >= 5
                    ? String.join(",", arr[0], arr[1], arr[2], arr[3], arr[4]) : "";
            default -> "";
        };
    }

    private String joinComma(String... nums) {
        List<String> padded = new ArrayList<>();
        for (String n : nums) padded.add(pad2(n));
        return String.join(",", padded);
    }

    // ===== ExtraInfo 构建 =====

    private String buildZhcwExtraInfo(JsonNode item) {
        String salesAmount = textValue(item, "saleMoney");
        String poolMoney = textValue(item, "prizePoolMoney");

        JsonNode winnerDetails = item.path("winnerDetails");
        String firstPrize = null, secondPrize = null;
        if (winnerDetails.isArray()) {
            for (JsonNode detail : winnerDetails) {
                String awardEtc = textValue(detail, "awardEtc");
                if ("1".equals(awardEtc)) firstPrize = summarizeWinnerDetail(detail);
                else if ("2".equals(awardEtc)) secondPrize = summarizeWinnerDetail(detail);
            }
        }

        String detail = joinNonBlank(DETAIL_SEPARATOR,
                prefixedValue("奖池", poolMoney),
                summarizeWinnerDetailsList(winnerDetails, 4));
        return buildExtraInfoJson(salesAmount, firstPrize, secondPrize, detail);
    }

    private String buildSportteryExtraInfo(JsonNode item) {
        String salesAmount = firstNonBlank(
                textValue(item, "totalSaleAmount"),
                textValue(item, "totalSaleAmountRj"));

        JsonNode prizeLevelList = item.path("prizeLevelList");
        String firstPrize = null, secondPrize = null;
        if (prizeLevelList.isArray()) {
            for (JsonNode prize : prizeLevelList) {
                String level = textValue(prize, "prizeLevel");
                if (firstPrize == null && level.startsWith("一等奖")) firstPrize = summarizePrizeLevel(prize);
                if (secondPrize == null && level.startsWith("二等奖")) secondPrize = summarizePrizeLevel(prize);
            }
        }

        String detail = joinNonBlank(DETAIL_SEPARATOR,
                prefixedValue("奖池", textValue(item, "poolBalanceAfterdraw")),
                prefixedValue("奖金滚入", textValue(item, "drawFlowFund")),
                summarizePrizeLevelList(prizeLevelList, 4));
        return buildExtraInfoJson(salesAmount, firstPrize, secondPrize, detail);
    }

    private String summarizeWinnerDetail(JsonNode detail) {
        JsonNode baseBetWinner = detail.path("baseBetWinner");
        String remark = textValue(baseBetWinner, "remark");
        String awardNum = textValue(baseBetWinner, "awardNum");
        String awardMoney = textValue(baseBetWinner, "awardMoney");
        String label = (remark != null && !remark.isBlank()) ? remark : (textValue(detail, "awardEtc") + "等奖");
        return joinNonBlank("，", blankToNull(label), suffixedValue(awardNum, "注"), suffixedValue(awardMoney, "元/注"));
    }

    private String summarizePrizeLevel(JsonNode prize) {
        String stakeCount = textValue(prize, "stakeCount");
        String stakeAmount = firstNonBlank(textValue(prize, "stakeAmount"), textValue(prize, "stakeAmountFormat"));
        return joinNonBlank("，",
                blankToNull(textValue(prize, "prizeLevel")),
                suffixedValue(stakeCount, "注"),
                suffixedValue(stakeAmount, "元/注"));
    }

    private String summarizeWinnerDetailsList(JsonNode details, int maxItems) {
        if (!details.isArray() || details.isEmpty()) return null;
        List<String> items = new ArrayList<>();
        for (JsonNode d : details) {
            String s = summarizeWinnerDetail(d);
            if (s != null) items.add(s);
            if (items.size() >= maxItems) break;
        }
        return items.isEmpty() ? null : String.join(DETAIL_SEPARATOR, items);
    }

    private String summarizePrizeLevelList(JsonNode list, int maxItems) {
        if (!list.isArray() || list.isEmpty()) return null;
        List<String> items = new ArrayList<>();
        for (JsonNode p : list) {
            String s = summarizePrizeLevel(p);
            if (s != null) items.add(s);
            if (items.size() >= maxItems) break;
        }
        return items.isEmpty() ? null : String.join(DETAIL_SEPARATOR, items);
    }

    private String buildExtraInfoJson(String salesAmount, String firstPrize, String secondPrize, Object detail) {
        Map<String, Object> extra = new LinkedHashMap<>();
        putIfPresent(extra, EXTRA_KEY_SALES_AMOUNT, salesAmount);
        putIfPresent(extra, EXTRA_KEY_FIRST_PRIZE, firstPrize);
        putIfPresent(extra, EXTRA_KEY_SECOND_PRIZE, secondPrize);
        putIfPresent(extra, EXTRA_KEY_DETAIL, detail);
        if (extra.isEmpty()) return null;
        try { return objectMapper.writeValueAsString(extra); }
        catch (Exception e) { throw new IllegalStateException("extraInfo 序列化失败", e); }
    }

    // ===== JSON 结构解析 =====

    private JsonNode resolveItems(JsonNode root) {
        JsonNode dataNode = root.path("data");
        if (dataNode.isObject() && dataNode.has("recordList")) return dataNode.path("recordList");
        if (dataNode.isArray()) return dataNode;
        if (root.has("value") && root.path("value").has("list")) return root.path("value").path("list");
        return objectMapper.createArrayNode();
    }

    private int resolveTotalPages(JsonNode root) {
        if (root.has("pages")) return root.path("pages").asInt(0);
        JsonNode dataNode = root.path("data");
        if (dataNode.isObject()) return dataNode.path("totalPage").asInt(dataNode.path("pages").asInt(0));
        if (root.has("value")) return root.path("value").path("pages").asInt(0);
        return 0;
    }

    private String getLotteryId(String type) {
        return switch (type) {
            case "dlt" -> "281";
            case "pl3" -> "283";
            case "pl5" -> "284";
            default -> null;
        };
    }

    // ===== HTTP 请求 =====

    private String sendGet(String url, String referer) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", UA)
                .header("Accept", "application/json,text/plain,*/*")
                .timeout(Duration.ofSeconds(20))
                .GET();
        if (referer != null && !referer.isBlank()) {
            builder.header("Referer", referer);
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    // ===== 工具方法 (委托 FetcherUtils) =====

    private void flushBatch(List<LotteryResult> batch, AtomicInteger inserted, AtomicInteger updated) {
        FetcherUtils.flushBatch(batch, resultService, inserted, updated);
    }

    private String textValue(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) return "";
        JsonNode v = node.path(fieldName);
        if (v.isMissingNode() || v.isNull()) return "";
        return (v.isValueNode() ? v.asText("") : v.toString()).trim();
    }

    private String normalizeDate(String raw) {
        return FetcherUtils.normalizeDate(raw);
    }

    private String pad2(String num) {
        return FetcherUtils.pad2(num);
    }

    private String firstNonBlank(String... values) {
        return FetcherUtils.firstNonBlank(values);
    }

    private String blankToNull(String v) {
        return FetcherUtils.blankToNull(v);
    }

    private String prefixedValue(String prefix, String value) {
        return FetcherUtils.prefixedValue(prefix, value);
    }

    private String suffixedValue(String value, String suffix) {
        return FetcherUtils.suffixedValue(value, suffix);
    }

    private String joinNonBlank(String delimiter, String... values) {
        return FetcherUtils.joinNonBlank(delimiter, values);
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        FetcherUtils.putIfPresent(target, key, value);
    }

    private void sleepQuietly(long millis) {
        FetcherUtils.sleepQuietly(millis);
    }

    /** 判断是否为可恢复的网络错误（5xx、超时、连接拒绝等） */
    private boolean isRecoverable(Exception e) {
        String msg = e.getMessage();
        if (msg != null && msg.startsWith("HTTP ")) {
            try {
                int status = Integer.parseInt(msg.substring(5).trim());
                return status >= 500;
            } catch (NumberFormatException ignored) {}
        }
        String className = e.getClass().getSimpleName();
        return className.contains("Timeout")
                || className.contains("ConnectException")
                || className.contains("IOException");
    }
}
