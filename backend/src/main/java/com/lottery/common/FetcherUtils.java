package com.lottery.common;

import com.lottery.entity.LotteryResult;
import com.lottery.service.LotteryResultService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据抓取公共工具类 —— 消除 ZhcwHtmlFetcher 和 ZhcwJsonFetcher 的重复代码
 */
public final class FetcherUtils {

    private FetcherUtils() {}

    // ===== 号码格式化 =====

    public static String pad2(String num) {
        return String.format("%02d", Integer.parseInt(num.trim()));
    }

    public static String joinWithComma(List<String> nums) {
        List<String> padded = new ArrayList<>();
        for (String num : nums) padded.add(pad2(num));
        return String.join(",", padded);
    }

    // ===== 字符串工具 =====

    public static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }

    public static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    public static String prefixedValue(String prefix, String value) {
        String n = blankToNull(value);
        return n == null ? null : prefix + n;
    }

    public static String suffixedValue(String value, String suffix) {
        String n = blankToNull(value);
        return n == null ? null : n + suffix;
    }

    public static String joinNonBlank(String delimiter, String... values) {
        List<String> parts = new ArrayList<>();
        for (String v : values) {
            if (v != null && !v.isBlank()) parts.add(v.trim());
        }
        return parts.isEmpty() ? null : String.join(delimiter, parts);
    }

    public static String normalizeDate(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String t = raw.trim();
        return t.length() >= 10 ? t.substring(0, 10) : t;
    }

    // ===== Map 工具 =====

    public static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value == null) return;
        if (value instanceof String s) {
            if (!s.isBlank()) target.put(key, s);
            return;
        }
        if (value instanceof Map<?, ?> m && m.isEmpty()) return;
        if (value instanceof List<?> l && l.isEmpty()) return;
        target.put(key, value);
    }

    // ===== 批量写入 =====

    public static void flushBatch(List<LotteryResult> batch, LotteryResultService resultService,
                                  AtomicInteger inserted, AtomicInteger updated) {
        if (batch.isEmpty()) return;
        for (LotteryResult result : batch) {
            LotteryResultService.SaveOutcome outcome = resultService.saveReal(result);
            if (outcome == LotteryResultService.SaveOutcome.INSERTED) inserted.incrementAndGet();
            else if (outcome == LotteryResultService.SaveOutcome.UPDATED) updated.incrementAndGet();
        }
        batch.clear();
    }

    // ===== 线程工具 =====

    /** 安全休眠，中断时重新设置标志（不抛异常） */
    public static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
