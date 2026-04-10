package com.lottery.common;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 彩票号码解析与统计工具类
 */
public final class LotteryNumberUtils {

    private LotteryNumberUtils() {}

    // ===== 双色球解析 =====

    public record ParsedSsq(int[] reds, int[] blue) {}

    public static int[] parseSsqReds(String numbers) {
        String[] parts = numbers.replace(" ", "").split("\\+");
        return Arrays.stream(parts[0].split(","))
                .filter(s -> !s.isBlank()).mapToInt(Integer::parseInt).toArray();
    }

    public static int[] parseSsqBlue(String numbers) {
        String[] parts = numbers.replace(" ", "").split("\\+");
        return parts.length > 1
                ? Arrays.stream(parts[1].split(",")).filter(s -> !s.isBlank()).mapToInt(Integer::parseInt).toArray()
                : new int[0];
    }

    public static ParsedSsq parseSsq(String numbers) {
        return new ParsedSsq(parseSsqReds(numbers), parseSsqBlue(numbers));
    }

    // ===== 大乐透解析 =====

    public record ParsedDlt(int[] front, int[] back) {}

    public static int[] parseDltFront(String numbers) {
        String[] parts = numbers.replace(" ", "").split("\\+");
        return Arrays.stream(parts[0].split(","))
                .filter(s -> !s.isBlank()).mapToInt(Integer::parseInt).toArray();
    }

    public static int[] parseDltBack(String numbers) {
        String[] parts = numbers.replace(" ", "").split("\\+");
        return parts.length > 1
                ? Arrays.stream(parts[1].split(",")).filter(s -> !s.isBlank()).mapToInt(Integer::parseInt).toArray()
                : new int[0];
    }

    public static ParsedDlt parseDlt(String numbers) {
        return new ParsedDlt(parseDltFront(numbers), parseDltBack(numbers));
    }

    // ===== 位置型解析 =====

    public static int[] parsePositional(String numbers, int count) {
        String cleaned = numbers.replace(",", "").replace(" ", "");
        int[] result = new int[count];
        for (int i = 0; i < count && i < cleaned.length(); i++) {
            result[i] = Character.getNumericValue(cleaned.charAt(i));
        }
        return result;
    }

    // ===== 通用解析 =====

    public static int[] parseGeneric(String numbers, int count) {
        return Arrays.stream(numbers.replace(",", " ").trim().split("\\s+"))
                .filter(s -> !s.isBlank() && s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .limit(count)
                .toArray();
    }

    // ===== 统计方法 =====

    /**
     * AC值 = 不同差值的个数 - (n-1)，衡量号码组合的复杂度
     */
    public static int calcAC(int[] nums) {
        Set<Integer> diffs = new HashSet<>();
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                diffs.add(Math.abs(nums[i] - nums[j]));
            }
        }
        return diffs.size() - (nums.length - 1);
    }

    /**
     * 连号个数: 有多少对相邻号码
     */
    public static int countConsecutive(int[] sortedNums) {
        int count = 0;
        for (int i = 1; i < sortedNums.length; i++) {
            if (sortedNums[i] - sortedNums[i - 1] == 1) count++;
        }
        return count;
    }
}
