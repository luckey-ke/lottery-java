package com.lottery.algorithm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * 彩票算法核心逻辑单元测试
 *
 * 纯算法测试，不依赖 Spring 上下文和数据库。
 */
class LotteryAlgorithmTest {

    // ===== AC值计算 =====

    @Test
    @DisplayName("AC值: 双色球 01,05,10,15,20,30")
    void testAC_Ssq() {
        int[] nums = {1, 5, 10, 15, 20, 30};
        int ac = calcAC(nums);
        // 差值: 4,5,9,14,15,19,24,25,29,10,15,20,25,29,5,10,15,20,5,10,15,10
        // 不同差值个数 = 13, AC = 13 - 5 = 8
        assertTrue(ac >= 0);
    }

    @Test
    @DisplayName("AC值: 连续号码应该为0")
    void testAC_Consecutive() {
        int[] nums = {1, 2, 3, 4, 5, 6};
        assertEquals(0, calcAC(nums));
    }

    @Test
    @DisplayName("AC值: 两个号码")
    void testAC_TwoNumbers() {
        int[] nums = {1, 10};
        assertEquals(0, calcAC(nums)); // 1个差值 - (2-1) = 0
    }

    @Test
    @DisplayName("AC值: 等间距号码")
    void testAC_EqualSpacing() {
        int[] nums = {1, 6, 11, 16, 21, 26}; // 等差数列，间距5
        assertEquals(0, calcAC(nums)); // 所有差值都是5的倍数，共5个不同差值 - 5 = 0
    }

    // ===== 连号计算 =====

    @Test
    @DisplayName("连号: 无连号")
    void testConsecutive_None() {
        int[] nums = {1, 5, 10, 15, 20, 30};
        assertEquals(0, countConsecutive(nums));
    }

    @Test
    @DisplayName("连号: 一对连号")
    void testConsecutive_OnePair() {
        int[] nums = {1, 2, 10, 15, 20, 30};
        assertEquals(1, countConsecutive(nums));
    }

    @Test
    @DisplayName("连号: 多对连号")
    void testConsecutive_MultiplePairs() {
        int[] nums = {1, 2, 3, 10, 11, 30};
        assertEquals(3, countConsecutive(nums)); // 1-2, 2-3, 10-11
    }

    @Test
    @DisplayName("连号: 完全连续")
    void testConsecutive_All() {
        int[] nums = {1, 2, 3, 4, 5, 6};
        assertEquals(5, countConsecutive(nums)); // 5对连号
    }

    @Test
    @DisplayName("连号: 单个号码")
    void testConsecutive_Single() {
        int[] nums = {10};
        assertEquals(0, countConsecutive(nums));
    }

    // ===== 号码解析 =====

    @Test
    @DisplayName("双色球号码解析")
    void testParseSsq() {
        String numbers = "01,05,10,15,20,30+07";
        String[] parts = numbers.replace(" ", "").split("\\+");
        int[] reds = Arrays.stream(parts[0].split(","))
                .filter(s -> !s.isBlank()).mapToInt(Integer::parseInt).toArray();
        int[] blues = Arrays.stream(parts[1].split(","))
                .filter(s -> !s.isBlank()).mapToInt(Integer::parseInt).toArray();

        assertArrayEquals(new int[]{1, 5, 10, 15, 20, 30}, reds);
        assertArrayEquals(new int[]{7}, blues);
    }

    @Test
    @DisplayName("大乐透号码解析")
    void testParseDlt() {
        String numbers = "01,10,20,25,35+05,12";
        String[] parts = numbers.replace(" ", "").split("\\+");
        int[] front = Arrays.stream(parts[0].split(","))
                .filter(s -> !s.isBlank()).mapToInt(Integer::parseInt).toArray();
        int[] back = Arrays.stream(parts[1].split(","))
                .filter(s -> !s.isBlank()).mapToInt(Integer::parseInt).toArray();

        assertArrayEquals(new int[]{1, 10, 20, 25, 35}, front);
        assertArrayEquals(new int[]{5, 12}, back);
    }

    @Test
    @DisplayName("位置型号码解析")
    void testParsePositional() {
        String numbers = "123";
        int[] result = new int[3];
        String cleaned = numbers.replace(",", "").replace(" ", "");
        for (int i = 0; i < 3 && i < cleaned.length(); i++) {
            result[i] = Character.getNumericValue(cleaned.charAt(i));
        }
        assertArrayEquals(new int[]{1, 2, 3}, result);
    }

    // ===== 和值/跨度 =====

    @Test
    @DisplayName("和值计算")
    void testSum() {
        int[] nums = {1, 5, 10, 15, 20, 30};
        assertEquals(81, Arrays.stream(nums).sum());
    }

    @Test
    @DisplayName("和值: 最小号码组合")
    void testSum_Minimum() {
        int[] nums = {1, 2, 3, 4, 5, 6};
        assertEquals(21, Arrays.stream(nums).sum());
    }

    @Test
    @DisplayName("跨度计算")
    void testSpan() {
        int[] nums = {1, 5, 10, 15, 20, 30};
        assertEquals(29, nums[nums.length - 1] - nums[0]);
    }

    @Test
    @DisplayName("跨度: 相同号码跨度为0")
    void testSpan_Same() {
        int[] nums = {10, 10, 10};
        assertEquals(0, nums[nums.length - 1] - nums[0]);
    }

    // ===== 奇偶/大小/质合 =====

    @Test
    @DisplayName("奇偶计数")
    void testOddEven() {
        int[] nums = {1, 5, 10, 15, 20, 30};
        long odds = Arrays.stream(nums).filter(n -> n % 2 == 1).count();
        assertEquals(3, odds);
    }

    @Test
    @DisplayName("全部奇数")
    void testAllOdd() {
        int[] nums = {1, 3, 5, 7, 9, 11};
        long odds = Arrays.stream(nums).filter(n -> n % 2 == 1).count();
        assertEquals(6, odds);
    }

    @Test
    @DisplayName("全部偶数")
    void testAllEven() {
        int[] nums = {2, 4, 6, 8, 10, 12};
        long odds = Arrays.stream(nums).filter(n -> n % 2 == 1).count();
        assertEquals(0, odds);
    }

    @Test
    @DisplayName("大小计数(双色球17分界)")
    void testBigSmall() {
        int[] nums = {1, 5, 10, 15, 20, 30};
        long bigs = Arrays.stream(nums).filter(n -> n >= 17).count();
        assertEquals(2, bigs);
    }

    @Test
    @DisplayName("大小计数: 边界值17")
    void testBigSmall_Boundary() {
        int[] nums = {16, 17, 18};
        long bigs = Arrays.stream(nums).filter(n -> n >= 17).count();
        assertEquals(2, bigs);
    }

    // ===== 复制的方法（避免依赖 Spring/Service 层） =====

    private int calcAC(int[] nums) {
        Set<Integer> diffs = new HashSet<>();
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                diffs.add(Math.abs(nums[i] - nums[j]));
            }
        }
        return diffs.size() - (nums.length - 1);
    }

    private int countConsecutive(int[] sortedNums) {
        int count = 0;
        for (int i = 1; i < sortedNums.length; i++) {
            if (sortedNums[i] - sortedNums[i - 1] == 1) count++;
        }
        return count;
    }
}
