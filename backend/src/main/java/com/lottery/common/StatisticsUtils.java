package com.lottery.common;

import com.lottery.common.LotteryNumberUtils.ParsedDlt;
import com.lottery.common.LotteryNumberUtils.ParsedSsq;
import com.lottery.entity.LotteryResult;

import java.util.*;

/**
 * 彩票统计计算工具类 —— 消除 AnalysisService 和 RecommendationService 中的重复逻辑
 */
public final class StatisticsUtils {

    private StatisticsUtils() {}

    // ===== 频率计算 =====

    /** 计算双色球红球频率 (1-max) */
    public static int[] calcSsqRedFreq(List<LotteryResult> rows, int max) {
        int[] freq = new int[max + 1];
        for (LotteryResult row : rows) {
            for (int n : LotteryNumberUtils.parseSsqReds(row.getNumbers())) {
                if (n >= 1 && n <= max) freq[n]++;
            }
        }
        return freq;
    }

    /** 计算双色球蓝球频率 */
    public static int[] calcSsqBlueFreq(List<LotteryResult> rows) {
        int[] freq = new int[17];
        for (LotteryResult row : rows) {
            for (int b : LotteryNumberUtils.parseSsqBlue(row.getNumbers())) {
                if (b >= 1 && b <= 16) freq[b]++;
            }
        }
        return freq;
    }

    /** 计算大乐透前区频率 */
    public static int[] calcDltFrontFreq(List<LotteryResult> rows) {
        int[] freq = new int[36];
        for (LotteryResult row : rows) {
            for (int f : LotteryNumberUtils.parseDltFront(row.getNumbers())) {
                if (f >= 1 && f <= 35) freq[f]++;
            }
        }
        return freq;
    }

    /** 计算大乐透后区频率 */
    public static int[] calcDltBackFreq(List<LotteryResult> rows) {
        int[] freq = new int[13];
        for (LotteryResult row : rows) {
            for (int b : LotteryNumberUtils.parseDltBack(row.getNumbers())) {
                if (b >= 1 && b <= 12) freq[b]++;
            }
        }
        return freq;
    }

    /** 计算七乐彩频率 */
    public static int[] calcQlcFreq(List<LotteryResult> rows) {
        int[] freq = new int[31];
        for (LotteryResult row : rows) {
            for (int n : LotteryNumberUtils.parseGeneric(row.getNumbers(), 7)) {
                if (n >= 1 && n <= 30) freq[n]++;
            }
        }
        return freq;
    }

    // ===== 遗漏值计算 =====

    /** 计算双色球红球遗漏值 */
    public static int[] calcSsqRedMissing(List<LotteryResult> rows) {
        return calcMissing(rows, true, 33);
    }

    /** 计算双色球蓝球遗漏值 */
    public static int[] calcSsqBlueMissing(List<LotteryResult> rows) {
        int[] miss = new int[17];
        for (LotteryResult row : rows) {
            Set<Integer> appeared = new HashSet<>();
            for (int b : LotteryNumberUtils.parseSsqBlue(row.getNumbers())) appeared.add(b);
            for (int n = 1; n <= 16; n++) {
                if (appeared.contains(n)) miss[n] = 0;
                else miss[n]++;
            }
        }
        return miss;
    }

    /** 通用遗漏值计算 (SSQ红球等) */
    public static int[] calcMissing(List<LotteryResult> rows, boolean isRed, int max) {
        int[] miss = new int[max + 1];
        for (LotteryResult row : rows) {
            Set<Integer> appeared = new HashSet<>();
            for (int n : LotteryNumberUtils.parseSsqReds(row.getNumbers())) appeared.add(n);
            for (int n = 1; n <= max; n++) {
                if (appeared.contains(n)) miss[n] = 0;
                else miss[n]++;
            }
        }
        return miss;
    }

    /** 计算七乐彩遗漏值 */
    public static int[] calcQlcMissing(List<LotteryResult> rows) {
        int[] miss = new int[31];
        for (LotteryResult row : rows) {
            Set<Integer> appeared = new HashSet<>();
            for (int n : LotteryNumberUtils.parseGeneric(row.getNumbers(), 7)) appeared.add(n);
            for (int n = 1; n <= 30; n++) {
                if (appeared.contains(n)) miss[n] = 0;
                else miss[n]++;
            }
        }
        return miss;
    }

    // ===== TopN 排序 =====

    /** 获取频率最高的/最低的 N 个号码 */
    public static List<String> topN(int[] freq, int min, int max, int n, boolean hot) {
        List<int[]> items = new ArrayList<>();
        for (int i = min; i <= max; i++) items.add(new int[]{i, freq[i]});
        items.sort(hot
                ? (a, b) -> Integer.compare(b[1], a[1])
                : (a, b) -> Integer.compare(a[1], b[1]));
        return items.subList(0, Math.min(n, items.size())).stream()
                .map(a -> String.format("%02d", a[0])).toList();
    }

    /** 获取频率最高的/最低的 N 个索引 */
    public static List<Integer> topNIndices(int[] freq, int min, int max, int n, boolean hot) {
        List<int[]> items = new ArrayList<>();
        for (int i = min; i <= max; i++) items.add(new int[]{i, freq[i]});
        items.sort(hot
                ? (a, b) -> Integer.compare(b[1], a[1])
                : (a, b) -> Integer.compare(a[1], b[1]));
        return items.subList(0, Math.min(n, items.size())).stream().map(a -> a[0]).toList();
    }

    /** 0-9 位的 TopN */
    public static List<String> topN10(int[] freq, int n, boolean hot) {
        List<int[]> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) items.add(new int[]{i, freq[i]});
        items.sort(hot
                ? (a, b) -> Integer.compare(b[1], a[1])
                : (a, b) -> Integer.compare(a[1], b[1]));
        return items.subList(0, Math.min(n, items.size())).stream()
                .map(a -> String.valueOf(a[0])).toList();
    }

    // ===== AC值 =====

    /** AC值 = 不同差值的个数 - (n-1) */
    public static int calcAC(int[] nums) {
        return LotteryNumberUtils.calcAC(nums);
    }

    /** AC值 (List 版本) */
    public static int calcAC(List<Integer> nums) {
        return LotteryNumberUtils.calcAC(nums.stream().mapToInt(Integer::intValue).toArray());
    }
}
