package com.lottery.common;

import java.util.*;

/**
 * 通用统计分析工具 —— 消除 AnalysisService 中各彩种分析方法的重复逻辑
 */
public final class AnalysisUtils {

    private static final Set<Integer> PRIMES = Set.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31);

    private AnalysisUtils() {}

    /**
     * 统计分布：输入整数列表，按值分桶计数
     */
    public static Map<Integer, Integer> distribution(List<Integer> values) {
        Map<Integer, Integer> dist = new LinkedHashMap<>();
        for (int v : values) dist.merge(v, 1, Integer::sum);
        return dist;
    }

    /**
     * 将 int[] 转为频率数组 (freq[value]++)
     */
    public static int[] calcFreq(int[] numbers, int max) {
        int[] freq = new int[max + 1];
        for (int n : numbers) {
            if (n >= 1 && n <= max) freq[n]++;
        }
        return freq;
    }

    /**
     * 奇偶比统计
     */
    public static String oddEvenRatio(int[] numbers) {
        long odds = Arrays.stream(numbers).filter(n -> n % 2 == 1).count();
        return odds + ":" + (numbers.length - odds);
    }

    /**
     * 大小比统计（threshold 以上为大）
     */
    public static String sizeRatio(int[] numbers, int threshold) {
        long bigs = Arrays.stream(numbers).filter(n -> n >= threshold).count();
        return bigs + ":" + (numbers.length - bigs);
    }

    /**
     * 质合比
     */
    public static String primeCompositeRatio(int[] numbers) {
        long primes = Arrays.stream(numbers).filter(PRIMES::contains).count();
        return primes + ":" + (numbers.length - primes);
    }

    /**
     * 012 路比
     */
    public static String mod012Ratio(int[] numbers) {
        long mod0 = Arrays.stream(numbers).filter(n -> n % 3 == 0).count();
        long mod1 = Arrays.stream(numbers).filter(n -> n % 3 == 1).count();
        long mod2 = Arrays.stream(numbers).filter(n -> n % 3 == 2).count();
        return mod0 + ":" + mod1 + ":" + mod2;
    }

    /**
     * 三区间分布
     */
    public static String zone3Ratio(int[] numbers, int z1Max, int z2Max) {
        long z1 = Arrays.stream(numbers).filter(n -> n <= z1Max).count();
        long z2 = Arrays.stream(numbers).filter(n -> n >= z1Max + 1 && n <= z2Max).count();
        long z3 = Arrays.stream(numbers).filter(n - 1 >= z2Max).count();
        return z1 + ":" + z2 + ":" + z3;
    }

    /**
     * 连号个数
     */
    public static int countConsecutive(int[] sortedNums) {
        return LotteryNumberUtils.countConsecutive(sortedNums);
    }

    /**
     * AC 值
     */
    public static int calcAC(int[] nums) {
        return LotteryNumberUtils.calcAC(nums);
    }

    /**
     * 重号：当前期与上一期相同号码个数
     */
    public static int countRepeats(int[] current, int[] previous) {
        Set<Integer> prevSet = new HashSet<>();
        for (int n : previous) prevSet.add(n);
        int count = 0;
        for (int n : current) {
            if (prevSet.contains(n)) count++;
        }
        return count;
    }

    /**
     * 热号 Top N
     */
    public static List<String> hotNumbers(int[] freq, int min, int max, int n) {
        return StatisticsUtils.topNIndices(freq, min, max, n, true)
                .stream().map(i -> String.format("%02d", i)).toList();
    }

    /**
     * 冷号 Top N
     */
    public static List<String> coldNumbers(int[] freq, int min, int max, int n) {
        return StatisticsUtils.topNIndices(freq, min, max, n, false)
                .stream().map(i -> String.format("%02d", i)).toList();
    }

    /**
     * 通用均值/最小/最大
     */
    public static Map<String, Object> summaryStats(List<Integer> values) {
        if (values.isEmpty()) return Map.of("avg", 0, "min", 0, "max", 0);
        int sum = 0, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int v : values) {
            sum += v;
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("avg", Math.round((double) sum / values.size() * 10) / 10.0);
        stats.put("min", min);
        stats.put("max", max);
        return stats;
    }

    /**
     * 分布统计（String key 版本）
     */
    public static void mergeCount(Map<String, Integer> map, String key) {
        map.merge(key, 1, Integer::sum);
    }
}
