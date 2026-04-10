package com.lottery.service.impl.fetcher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottery.common.FetcherUtils;
import com.lottery.entity.LotteryResult;
import com.lottery.service.LotteryResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 中彩网 HTML 页面数据抓取器
 * <p>适用彩种：双色球(ssq)、福彩3D(fc3d)、七乐彩(qlc)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ZhcwHtmlFetcher {

    private static final String UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final Pattern PAGE_NUM_PATTERN = Pattern.compile("pageNum=(\\d+)");
    private static final int PAGE_SIZE = 200;
    private static final int FETCH_BATCH_SIZE = 200;
    private static final long PAGE_DELAY_MS = 1200;
    private static final long RETRY_DELAY_MS = 2000;
    private static final int MAX_EMPTY_RETRIES = 3;

    private static final String DETAIL_LABEL_INFO = "详细信息";
    private static final String DETAIL_LABEL_VIDEO = "开奖视频";
    private static final String EXTRA_KEY_SALES_AMOUNT = "salesAmount";
    private static final String EXTRA_KEY_FIRST_PRIZE = "firstPrize";
    private static final String EXTRA_KEY_SECOND_PRIZE = "secondPrize";
    private static final String EXTRA_KEY_DETAIL = "detail";
    private static final String DETAIL_SEPARATOR = "；";
    private static final String SLASH_SEPARATOR = " / ";

    private static final String ZHCW_SSQ_DETAIL_URL = "https://www.zhcw.com/kjxx/ssq/";
    private static final String ZHCW_SSQ_VIDEO_URL = "https://www.zhcw.com/spzb/kjspzb/";
    private static final String ZHCW_FC3D_DETAIL_URL = "https://www.zhcw.com/kjxx/3d/";
    private static final String ZHCW_FC3D_VIDEO_URL = "https://www.zhcw.com/spzb/kjspzb/";
    private static final String ZHCW_QLC_DETAIL_URL = "https://www.zhcw.com/kjxx/qlc/";
    private static final String ZHCW_QLC_VIDEO_URL = "https://www.zhcw.com/spzb/kjspzb/";

    private final LotteryResultService resultService;
    private final ObjectMapper objectMapper;

    /**
     * 抓取指定彩种的开奖数据
     *
     * @param type      彩种代号 (ssq/fc3d/qlc)
     * @param limitCount 限制条数，null 表示不限
     * @param cutoffDate 截止日期，null 表示不限
     * @param fetchAll  是否拉取全部
     * @param progressCallback 进度回调 (currentPage, totalFetched, inserted, updated)
     * @return [totalFetched, inserted, updated]
     */
    public int[] fetch(String type, Integer limitCount, String cutoffDate, boolean fetchAll,
                       FetchProgressCallback progressCallback) {
        String pageUrlPattern = switch (type) {
            case "ssq" -> "https://kaijiang.zhcw.com/zhcw/inc/ssq/ssq_wqhg.jsp?pageNum=%d";
            case "fc3d" -> "https://kaijiang.zhcw.com/zhcw/inc/3d/3d_wqhg.jsp?pageNum=%d";
            case "qlc" -> "https://kaijiang.zhcw.com/zhcw/inc/qlc/qlc_wqhg.jsp?pageNum=%d";
            default -> throw new IllegalArgumentException("不支持的彩种: " + type);
        };

        AtomicInteger total = new AtomicInteger();
        AtomicInteger inserted = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();
        List<LotteryResult> batch = new ArrayList<>();
        Set<String> seen = new TreeSet<>();
        int pageNo = 1;
        int emptyRetryCount = 0;
        boolean stop = false;

        while (!stop) {
            if (progressCallback != null) {
                progressCallback.onProgress(pageNo, total.get(), inserted.get(), updated.get());
            }

            try {
                String url = pageUrlPattern.formatted(pageNo);
                Document doc = Jsoup.connect(url)
                        .userAgent(UA)
                        .timeout(20_000)
                        .get();

                List<LotteryResult> pageResults = parseHtmlTable(type, doc);
                int lastPage = extractLastPageNo(doc);

                if (pageResults.isEmpty()) {
                    if (pageNo <= lastPage && emptyRetryCount < MAX_EMPTY_RETRIES) {
                        emptyRetryCount++;
                        log.warn("中彩网第 {} 页 [{}] 返回空数据，重试 {}/{}", pageNo, type, emptyRetryCount, MAX_EMPTY_RETRIES);
                        sleepQuietly(RETRY_DELAY_MS * emptyRetryCount);
                        continue;
                    }
                    break;
                }
                emptyRetryCount = 0;

                for (LotteryResult result : pageResults) {
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
                if (lastPage > 0 && pageNo >= lastPage) break;
                pageNo++;
                sleepQuietly(PAGE_DELAY_MS);

            } catch (Exception e) {
                log.warn("中彩网页面请求失败 ({}) : {}", type, e.getMessage(), e);
                break;
            }
        }

        flushBatch(batch, inserted, updated);
        log.info("中彩网HTML获取到 {} 条 [{}]", total.get(), type);
        return new int[]{total.get(), inserted.get(), updated.get()};
    }

    private List<LotteryResult> parseHtmlTable(String type, Document doc) {
        List<LotteryResult> list = new ArrayList<>();
        Elements rows = doc.select("table tr");
        for (Element row : rows) {
            Elements cells = row.select("td, th");
            if (cells.size() < 3) continue;

            String drawDate = cells.get(0).text().trim();
            String drawNum = cells.get(1).text().trim();
            if (!drawDate.matches("\\d{4}-\\d{2}-\\d{2}") || !drawNum.matches("\\d+")) continue;

            Elements ems = cells.get(2).select("em");
            List<String> nums = ems.eachText();
            String numbers = formatNumbers(type, nums);
            if (drawDate.isBlank() || drawNum.isBlank() || numbers.isBlank()) continue;

            LotteryResult result = new LotteryResult();
            result.setLotteryType(type);
            result.setDrawNum(drawNum);
            result.setDrawDate(drawDate);
            result.setNumbers(numbers);
            result.setExtraInfo(buildExtraInfo(type, cells));
            list.add(result);
        }
        return list;
    }

    private String formatNumbers(String type, List<String> nums) {
        return switch (type) {
            case "ssq" -> nums.size() >= 7
                    ? joinWithComma(nums.subList(0, 6)) + "+" + pad2(nums.get(6)) : "";
            case "fc3d" -> nums.size() >= 3
                    ? String.join(",", nums.subList(0, 3)) : "";
            case "qlc" -> nums.size() >= 8
                    ? joinWithComma(nums.subList(0, 7)) + "+" + pad2(nums.get(8 - 1))
                    : nums.size() >= 7 ? joinWithComma(nums.subList(0, 7)) : "";
            default -> "";
        };
    }

    private String buildExtraInfo(String type, Elements cells) {
        return switch (type) {
            case "ssq", "qlc" -> buildExtraInfoJson(
                    cellText(cells, 3), cellText(cells, 4), cellText(cells, 5),
                    resolveDetailValue(type, cells, 6));
            case "fc3d" -> buildExtraInfoJson(
                    cellText(cells, 6), cellText(cells, 3), cellText(cells, 4),
                    buildFc3dDetail(cells));
            default -> null;
        };
    }

    private Object resolveDetailValue(String type, Elements cells, int index) {
        Map<String, String> links = buildDetailLinks(type);
        String extraText = firstNonBlank(cellTitleText(cells, index), cellText(cells, index));
        if (links.isEmpty()) return extraText;

        Map<String, Object> detail = new LinkedHashMap<>();
        if (extraText != null && !extraText.isBlank()) detail.put("text", extraText);
        detail.put("links", links);
        return detail;
    }

    private Object buildFc3dDetail(Elements cells) {
        Object baseDetail = resolveDetailValue("fc3d", cells, 8);
        String extraText = joinNonBlank(DETAIL_SEPARATOR,
                prefixedValue("组选6:", cellText(cells, 5)),
                prefixedValue("返奖比例:", cellText(cells, 7)));
        if (baseDetail instanceof Map<?, ?> baseMap) {
            Map<String, Object> detailMap = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : baseMap.entrySet()) {
                detailMap.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            if (extraText != null && !extraText.isBlank()) {
                Object text = detailMap.get("text");
                detailMap.put("text", joinNonBlank(DETAIL_SEPARATOR,
                        text == null ? null : String.valueOf(text), extraText));
            }
            return detailMap;
        }
        return joinNonBlank(DETAIL_SEPARATOR, baseDetail == null ? null : String.valueOf(baseDetail), extraText);
    }

    private Map<String, String> buildDetailLinks(String type) {
        Map<String, String> links = new LinkedHashMap<>();
        switch (type) {
            case "ssq" -> {
                links.put(DETAIL_LABEL_INFO, ZHCW_SSQ_DETAIL_URL);
                links.put(DETAIL_LABEL_VIDEO, ZHCW_SSQ_VIDEO_URL);
            }
            case "fc3d" -> {
                links.put(DETAIL_LABEL_INFO, ZHCW_FC3D_DETAIL_URL);
                links.put(DETAIL_LABEL_VIDEO, ZHCW_FC3D_VIDEO_URL);
            }
            case "qlc" -> {
                links.put(DETAIL_LABEL_INFO, ZHCW_QLC_DETAIL_URL);
                links.put(DETAIL_LABEL_VIDEO, ZHCW_QLC_VIDEO_URL);
            }
        }
        return links;
    }

    private String buildExtraInfoJson(String salesAmount, String firstPrize, String secondPrize, Object detail) {
        Map<String, Object> extra = new LinkedHashMap<>();
        putIfPresent(extra, EXTRA_KEY_SALES_AMOUNT, salesAmount);
        putIfPresent(extra, EXTRA_KEY_FIRST_PRIZE, firstPrize);
        putIfPresent(extra, EXTRA_KEY_SECOND_PRIZE, secondPrize);
        putIfPresent(extra, EXTRA_KEY_DETAIL, detail);
        if (extra.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(extra);
        } catch (Exception e) {
            throw new IllegalStateException("extraInfo 序列化失败", e);
        }
    }

    // ===== HTML 解析工具 =====

    private int extractLastPageNo(Document doc) {
        int lastPage = 0;
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            Matcher matcher = PAGE_NUM_PATTERN.matcher(link.attr("href"));
            if (matcher.find()) lastPage = Math.max(lastPage, Integer.parseInt(matcher.group(1)));
        }
        for (Element el : doc.select("span, a")) {
            String text = el.text().trim();
            if (text.matches("\\d+")) {
                try {
                    int num = Integer.parseInt(text);
                    if (num > 0 && num < 10000) lastPage = Math.max(lastPage, num);
                } catch (NumberFormatException ignored) {}
            }
        }
        return lastPage;
    }

    private String extractTitleText(Element cell) {
        Elements links = cell.select("[title]");
        if (links.isEmpty()) return null;
        List<String> titles = new ArrayList<>();
        for (Element link : links) {
            String title = link.attr("title").trim();
            if (!title.isBlank() && !DETAIL_LABEL_INFO.equals(title) && !DETAIL_LABEL_VIDEO.equals(title)) {
                titles.add(title);
            }
        }
        return titles.isEmpty() ? null : String.join(SLASH_SEPARATOR, titles);
    }

    private String cellText(Elements cells, int index) {
        if (index < 0 || index >= cells.size()) return null;
        String text = cells.get(index).text().trim();
        return text.isBlank() ? null : text;
    }

    private String cellTitleText(Elements cells, int index) {
        if (index < 0 || index >= cells.size()) return null;
        return extractTitleText(cells.get(index));
    }

    private void flushBatch(List<LotteryResult> batch, AtomicInteger inserted, AtomicInteger updated) {
        if (batch.isEmpty()) return;
        for (LotteryResult result : batch) {
            LotteryResultService.SaveOutcome outcome = resultService.saveReal(result);
            if (outcome == LotteryResultService.SaveOutcome.INSERTED) inserted.incrementAndGet();
            else if (outcome == LotteryResultService.SaveOutcome.UPDATED) updated.incrementAndGet();
        }
        batch.clear();
    }

    // ===== 工具方法 (委托 FetcherUtils) =====

    private void flushBatch(List<LotteryResult> batch, AtomicInteger inserted, AtomicInteger updated) {
        FetcherUtils.flushBatch(batch, resultService, inserted, updated);
    }

    private String joinWithComma(List<String> nums) {
        return FetcherUtils.joinWithComma(nums);
    }

    private String pad2(String num) {
        return FetcherUtils.pad2(num);
    }

    private String firstNonBlank(String... values) {
        return FetcherUtils.firstNonBlank(values);
    }

    private String prefixedValue(String prefix, String value) {
        return FetcherUtils.prefixedValue(prefix, value);
    }

    private String joinNonBlank(String delimiter, String... values) {
        return FetcherUtils.joinNonBlank(delimiter, values);
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        FetcherUtils.putIfPresent(target, key, value);
    }

    /** 安全休眠，中断时抛出 IllegalStateException 终止抓取流程 */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待中断", e);
        }
    }
}
