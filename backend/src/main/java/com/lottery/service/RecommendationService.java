package com.lottery.service;

import com.lottery.common.LotteryNumberUtils;
import com.lottery.common.StatisticsUtils;
import com.lottery.entity.LotteryResult;
import com.lottery.entity.LotteryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 号码推荐服务 - 基于历史数据与统计分析，每日推荐5组号码。
 *
 * 策略说明：
 *   1. 均衡策略 - 热号+冷号+温号均衡搭配，奇偶/大小/区间合理分布
 *   2. 热号优先 - 侧重高频号码，跟踪近期趋势
 *   3. 冷号回补 - 侧重遗漏值大的号码，博冷门回补
 *   4. 统计最优 - AC值高、跨度适中、符合历史分布规律
 *   5. 机选增强 - 随机生成但满足基本统计约束
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final Set<Integer> PRIMES = Set.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String[] STRATEGIES = {"均衡策略", "热号追踪", "冷号回补", "统计最优", "随机精选"};

    private final LotteryResultService resultService;
    private final AnalysisService analysisService;
    private final RecommendationHistoryService historyService;

    public Map<String, Object> recommend(String type) {
        List<LotteryResult> rows = resultService.allRealNumbers(type);
        if (rows.isEmpty()) return Map.of("error", "[" + type + "] 暂无数据，无法推荐");

        // 自动匹配历史推荐
        try { historyService.autoMatch(type); } catch (Exception ignored) {}

        // 获取动态权重
        double[] weights = historyService.getStrategyWeights(type);
        long seed = LocalDate.now().toEpochDay() * 100 + type.hashCode() % 100;

        Map<String, Object> result = switch (type) {
            case "ssq" -> recommendSsq(rows, seed, weights);
            case "dlt" -> recommendDlt(rows, seed, weights);
            case "fc3d", "pl3" -> recommendPositional(rows, type, 3, seed, weights);
            case "pl5" -> recommendPositional(rows, type, 5, seed, weights);
            case "qlc" -> recommendQlc(rows, seed, weights);
            default -> Map.of("error", "未知彩种: " + type);
        };

        String today = LocalDate.now().format(DATE_FMT);
        result.put("date", today);
        result.put("lotteryType", type);
        result.put("name", LotteryType.fromCode(type).getName());

        // 保存到历史
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> groups = (List<Map<String, Object>>) result.get("groups");
        if (groups != null) {
            try { historyService.save(type, today, groups); } catch (Exception ignored) {}
        }

        return result;
    }

    // ============================================================
    //  双色球推荐
    // ============================================================

    private Map<String, Object> recommendSsq(List<LotteryResult> rows, long seed, double[] weights) {
        int[] redFreq = calcFreq(rows, true, 33);
        int[] blueFreq = calcFreqSsqBlue(rows);
        int[] redMissing = calcMissing(rows, true, 33);
        int[] blueMissing = calcMissingSsqBlue(rows);

        // 近30期频率
        List<LotteryResult> recent30 = tail(rows, 30);
        int[] recentRedFreq = calcFreq(recent30, true, 33);
        int[] recentBlueFreq = calcFreqSsqBlue(recent30);

        // 排序获取热冷号
        List<Integer> redHot = topNIndices(redFreq, 1, 33, 12, true);
        List<Integer> redCold = topNIndices(redFreq, 1, 33, 12, false);
        List<Integer> redRecentHot = topNIndices(recentRedFreq, 1, 33, 10, true);
        List<Integer> blueHot = topNIndices(blueFreq, 1, 16, 6, true);
        List<Integer> blueCold = topNIndices(blueFreq, 1, 16, 6, false);

        Random rnd = new Random(seed);
        List<Map<String, Object>> groups = new ArrayList<>();

        // 权重影响候选池大小：权重越高，候选越多，选择空间越大
        int hotBoost = (int) Math.round(weights[1] * 2);  // 热号权重
        int coldBoost = (int) Math.round(weights[2] * 2); // 冷号权重

        // 策略1: 均衡策略 (3热+2温+1冷)
        groups.add(buildSsqGroup("均衡策略", rnd,
                pickMix(redHot, redCold, redRecentHot, 3 + hotBoost / 2, 1 + coldBoost / 2, 2),
                blueHot, blueCold, 1));

        // 策略2: 热号优先 (4热+2温)
        groups.add(buildSsqGroup("热号追踪", rnd,
                pickMix(redHot, redCold, redRecentHot, 4 + hotBoost, 0, 2),
                blueHot, blueCold, 1));

        // 策略3: 冷号回补 (2热+1温+3冷)
        groups.add(buildSsqGroup("冷号回补", rnd,
                pickMix(redHot, redCold, redRecentHot, 2, 3 + coldBoost, 1),
                blueHot, blueCold, 1));

        // 策略4: 统计最优 (AC值高+跨度适中)
        groups.add(buildSsqGroupOptimal("统计最优", rnd, redFreq, redMissing, redHot, redCold, blueHot, blueCold));

        // 策略5: 机选增强
        groups.add(buildSsqGroupRandom("随机精选", rnd, redHot, redCold, blueHot, blueCold));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groups", groups);
        return result;
    }

    private Map<String, Object> buildSsqGroup(String strategy, Random rnd,
                                               List<Integer> redPool,
                                               List<Integer> blueHot, List<Integer> blueCold,
                                               int blueCount) {
        // 从候选池中选6个红球，确保AC值和跨度合理
        List<Integer> reds = selectWithConstraints(redPool, 6, 33, rnd, 2, 8);
        Collections.sort(reds);

        // 蓝球：从热号中选
        List<Integer> bluePool = new ArrayList<>(blueHot);
        bluePool.addAll(blueCold.subList(0, Math.min(2, blueCold.size())));
        List<Integer> blues = pickRandom(bluePool, blueCount, rnd);

        Map<String, Object> group = new LinkedHashMap<>();
        group.put("strategy", strategy);
        group.put("reds", reds.stream().map(n -> String.format("%02d", n)).toList());
        group.put("blues", blues.stream().map(n -> String.format("%02d", n)).toList());
        group.put("display", reds.stream().map(n -> String.format("%02d", n)).collect(Collectors.joining(" "))
                + " + " + blues.stream().map(n -> String.format("%02d", n)).collect(Collectors.joining(" ")));
        return group;
    }

    private Map<String, Object> buildSsqGroupOptimal(String strategy, Random rnd,
                                                      int[] freq, int[] missing,
                                                      List<Integer> redHot, List<Integer> redCold,
                                                      List<Integer> blueHot, List<Integer> blueCold) {
        // 综合评分：频率权重 + 遗漏权重，选综合得分最高的组合
        double[] scores = new double[34];
        for (int i = 1; i <= 33; i++) {
            scores[i] = freq[i] * 0.4 + missing[i] * 0.6; // 适当偏向遗漏值大的
        }

        List<Integer> candidates = new ArrayList<>();
        for (int i = 1; i <= 33; i++) candidates.add(i);
        candidates.sort((a, b) -> Double.compare(scores[b], scores[a]));

        List<Integer> reds = selectWithConstraints(candidates, 6, 33, rnd, 3, 10);
        Collections.sort(reds);

        List<Integer> bluePool = new ArrayList<>(blueHot);
        bluePool.addAll(blueCold.subList(0, Math.min(3, blueCold.size())));
        List<Integer> blues = pickRandom(bluePool, 1, rnd);

        Map<String, Object> group = new LinkedHashMap<>();
        group.put("strategy", strategy);
        group.put("reds", reds.stream().map(n -> String.format("%02d", n)).toList());
        group.put("blues", blues.stream().map(n -> String.format("%02d", n)).toList());
        group.put("display", reds.stream().map(n -> String.format("%02d", n)).collect(Collectors.joining(" "))
                + " + " + blues.stream().map(n -> String.format("%02d", n)).collect(Collectors.joining(" ")));
        return group;
    }

    private Map<String, Object> buildSsqGroupRandom(String strategy, Random rnd,
                                                     List<Integer> redHot, List<Integer> redCold,
                                                     List<Integer> blueHot, List<Integer> blueCold) {
        List<Integer> allReds = new ArrayList<>();
        for (int i = 1; i <= 33; i++) allReds.add(i);

        List<Integer> reds = selectWithConstraints(allReds, 6, 33, rnd, 2, 12);
        Collections.sort(reds);

        List<Integer> allBlues = new ArrayList<>();
        for (int i = 1; i <= 16; i++) allBlues.add(i);
        List<Integer> blues = pickRandom(allBlues, 1, rnd);

        Map<String, Object> group = new LinkedHashMap<>();
        group.put("strategy", strategy);
        group.put("reds", reds.stream().map(n -> String.format("%02d", n)).toList());
        group.put("blues", blues.stream().map(n -> String.format("%02d", n)).toList());
        group.put("display", reds.stream().map(n -> String.format("%02d", n)).collect(Collectors.joining(" "))
                + " + " + blues.stream().map(n -> String.format("%02d", n)).collect(Collectors.joining(" ")));
        return group;
    }

    // ============================================================
    //  大乐透推荐
    // ============================================================

    private Map<String, Object> recommendDlt(List<LotteryResult> rows, long seed, double[] weights) {
        int[] frontFreq = calcFreqDltFront(rows);
        int[] backFreq = calcFreqDltBack(rows);

        List<LotteryResult> recent30 = tail(rows, 30);
        int[] recentFrontFreq = calcFreqDltFront(recent30);

        List<Integer> frontHot = topNIndices(frontFreq, 1, 35, 12, true);
        List<Integer> frontCold = topNIndices(frontFreq, 1, 35, 12, false);
        List<Integer> frontRecentHot = topNIndices(recentFrontFreq, 1, 35, 10, true);
        List<Integer> backHot = topNIndices(backFreq, 1, 12, 5, true);
        List<Integer> backCold = topNIndices(backFreq, 1, 12, 5, false);

        Random rnd = new Random(seed);
        List<Map<String, Object>> groups = new ArrayList<>();

        int hotBoost = (int) Math.round(weights[1] * 2);
        int coldBoost = (int) Math.round(weights[2] * 2);

        groups.add(buildDltGroup("均衡策略", rnd, pickMix(frontHot, frontCold, frontRecentHot, 3 + hotBoost / 2, 1 + coldBoost / 2, 1), backHot, backCold));
        groups.add(buildDltGroup("热号追踪", rnd, pickMix(frontHot, frontCold, frontRecentHot, 4 + hotBoost, 0, 1), backHot, backCold));
        groups.add(buildDltGroup("冷号回补", rnd, pickMix(frontHot, frontCold, frontRecentHot, 1, 3 + coldBoost, 1), backHot, backCold));
        groups.add(buildDltGroup("统计最优", rnd, selectByScore(frontFreq, 5, 35, rnd), backHot, backCold));
        groups.add(buildDltGroup("随机精选", rnd, randomPool(1, 35), backHot, backCold));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groups", groups);
        return result;
    }

    private Map<String, Object> buildDltGroup(String strategy, Random rnd,
                                               List<Integer> frontPool,
                                               List<Integer> backHot, List<Integer> backCold) {
        List<Integer> fronts = selectWithConstraints(frontPool, 5, 35, rnd, 1, 15);
        Collections.sort(fronts);

        List<Integer> backPool = new ArrayList<>(backHot);
        backPool.addAll(backCold.subList(0, Math.min(3, backCold.size())));
        List<Integer> backs = pickRandom(backPool, 2, rnd);
        Collections.sort(backs);

        Map<String, Object> group = new LinkedHashMap<>();
        group.put("strategy", strategy);
        group.put("fronts", fronts.stream().map(n -> String.format("%02d", n)).toList());
        group.put("backs", backs.stream().map(n -> String.format("%02d", n)).toList());
        group.put("display", fronts.stream().map(n -> String.format("%02d", n)).collect(Collectors.joining(" "))
                + " + " + backs.stream().map(n -> String.format("%02d", n)).collect(Collectors.joining(" ")));
        return group;
    }

    // ============================================================
    //  位置型推荐 (3D / 排列三 / 排列五)
    // ============================================================

    private Map<String, Object> recommendPositional(List<LotteryResult> rows, String type, int positions, long seed, double[] weights) {
        // 各位频率
        int[][] posFreq = new int[positions][10];
        for (LotteryResult row : rows) {
            int[] nums = LotteryNumberUtils.parsePositional(row.getNumbers(), positions);
            for (int i = 0; i < positions; i++) posFreq[i][nums[i]]++;
        }

        // 近30期
        List<LotteryResult> recent30 = tail(rows, 30);
        int[][] recentPosFreq = new int[positions][10];
        for (LotteryResult row : recent30) {
            int[] nums = LotteryNumberUtils.parsePositional(row.getNumbers(), positions);
            for (int i = 0; i < positions; i++) recentPosFreq[i][nums[i]]++;
        }

        Random rnd = new Random(seed);
        List<Map<String, Object>> groups = new ArrayList<>();

        // 权重调整热号偏向
        double hotW = weights[1];
        double coldW = weights[2];

        // 策略1: 各位取热号
        groups.add(buildPositionalGroup("热号追踪", positions, posFreq, recentPosFreq, rnd, 0.5 + hotW * 0.15));
        // 策略2: 各位取冷号
        groups.add(buildPositionalGroup("冷号回补", positions, posFreq, recentPosFreq, rnd, 0.5 - coldW * 0.15));
        // 策略3: 均衡
        groups.add(buildPositionalGroup("均衡策略", positions, posFreq, recentPosFreq, rnd, 0.5));
        // 策略4: 近期趋势
        groups.add(buildPositionalGroup("近期趋势", positions, recentPosFreq, recentPosFreq, rnd, 0.6));
        // 策略5: 随机
        groups.add(buildPositionalGroup("随机精选", positions, posFreq, recentPosFreq, rnd, -1));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groups", groups);
        return result;
    }

    private Map<String, Object> buildPositionalGroup(String strategy, int positions,
                                                      int[][] posFreq, int[][] recentPosFreq,
                                                      Random rnd, double hotBias) {
        List<Integer> digits = new ArrayList<>();
        for (int i = 0; i < positions; i++) {
            if (hotBias < 0) {
                // 纯随机
                digits.add(rnd.nextInt(10));
            } else {
                // 加权随机
                double[] weights = new double[10];
                for (int d = 0; d < 10; d++) {
                    double allScore = posFreq[i][d];
                    double recentScore = recentPosFreq[i][d];
                    weights[d] = allScore * (1 - hotBias) + recentScore * hotBias + 1;
                }
                digits.add(weightedRandom(weights, rnd));
            }
        }

        Map<String, Object> group = new LinkedHashMap<>();
        group.put("strategy", strategy);
        group.put("digits", digits.stream().map(String::valueOf).toList());
        group.put("display", digits.stream().map(String::valueOf).collect(Collectors.joining(" ")));
        return group;
    }

    // ============================================================
    //  七乐彩推荐
    // ============================================================

    private Map<String, Object> recommendQlc(List<LotteryResult> rows, long seed, double[] weights) {
        int[] freq = calcFreqQlc(rows);
        int[] missing = calcMissingQlc(rows);

        List<LotteryResult> recent30 = tail(rows, 30);
        int[] recentFreq = calcFreqQlc(recent30);

        List<Integer> hot = topNIndices(freq, 1, 30, 12, true);
        List<Integer> cold = topNIndices(freq, 1, 30, 12, false);
        List<Integer> recentHot = topNIndices(recentFreq, 1, 30, 10, true);

        Random rnd = new Random(seed);
        List<Map<String, Object>> groups = new ArrayList<>();

        int hotBoost = (int) Math.round(weights[1] * 2);
        int coldBoost = (int) Math.round(weights[2] * 2);

        groups.add(buildQlcGroup("均衡策略", rnd, pickMix(hot, cold, recentHot, 3 + hotBoost / 2, 2 + coldBoost / 2, 2)));
        groups.add(buildQlcGroup("热号追踪", rnd, pickMix(hot, cold, recentHot, 5 + hotBoost, 0, 2)));
        groups.add(buildQlcGroup("冷号回补", rnd, pickMix(hot, cold, recentHot, 2, 4 + coldBoost, 1)));
        groups.add(buildQlcGroup("统计最优", rnd, selectByScore(freq, 7, 30, rnd)));
        groups.add(buildQlcGroup("随机精选", rnd, randomPool(1, 30)));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("groups", groups);
        return result;
    }

    private Map<String, Object> buildQlcGroup(String strategy, Random rnd, List<Integer> pool) {
        List<Integer> nums = selectWithConstraints(pool, 7, 30, rnd, 2, 10);
        Collections.sort(nums);

        Map<String, Object> group = new LinkedHashMap<>();
        group.put("strategy", strategy);
        group.put("numbers", nums.stream().map(n -> String.format("%02d", n)).toList());
        group.put("display", nums.stream().map(n -> String.format("%02d", n)).collect(Collectors.joining(" ")));
        return group;
    }

    // ============================================================
    //  通用工具方法
    // ============================================================

    /** 从池中选count个，满足AC值和跨度约束 */
    private List<Integer> selectWithConstraints(List<Integer> pool, int count, int maxVal, Random rnd,
                                                 int minAC, int maxSpan) {
        for (int attempt = 0; attempt < 200; attempt++) {
            List<Integer> picked = pickRandom(pool, count, rnd);
            Collections.sort(picked);
            int span = picked.get(picked.size() - 1) - picked.get(0);
            int ac = calcAC(picked);
            if (span >= 5 && span <= maxSpan * 3 && ac >= minAC) {
                return picked;
            }
        }
        // 兜底：直接从1-max中选
        List<Integer> all = new ArrayList<>();
        for (int i = 1; i <= maxVal; i++) all.add(i);
        List<Integer> picked = pickRandom(all, count, rnd);
        Collections.sort(picked);
        return picked;
    }

    /** 从列表中随机选count个 */
    private List<Integer> pickRandom(List<Integer> pool, int count, Random rnd) {
        List<Integer> copy = new ArrayList<>(pool);
        Collections.shuffle(copy, rnd);
        return new ArrayList<>(new TreeSet<>(copy.subList(0, Math.min(count, copy.size()))));
    }

    /** 混合选择：从热号取hot个，从冷号取cold个，从近期热号取trend个 */
    private List<Integer> pickMix(List<Integer> hot, List<Integer> cold, List<Integer> trend,
                                   int hotCount, int coldCount, int trendCount) {
        Set<Integer> result = new LinkedHashSet<>();
        addN(result, hot, hotCount);
        addN(result, cold, coldCount);
        addN(result, trend, trendCount);
        return new ArrayList<>(result);
    }

    private void addN(Set<Integer> target, List<Integer> source, int n) {
        for (int i = 0; i < Math.min(n, source.size()); i++) {
            target.add(source.get(i));
        }
    }

    /** 综合评分选择 */
    private List<Integer> selectByScore(int[] freq, int count, int max, Random rnd) {
        double[] scores = new double[max + 1];
        for (int i = 1; i <= max; i++) {
            scores[i] = freq[i] + rnd.nextDouble() * 3; // 加少量随机扰动
        }
        List<Integer> indices = new ArrayList<>();
        for (int i = 1; i <= max; i++) indices.add(i);
        indices.sort((a, b) -> Double.compare(scores[b], scores[a]));
        List<Integer> picked = new ArrayList<>(new TreeSet<>(indices.subList(0, Math.min(count + 3, indices.size()))));
        Collections.shuffle(picked, rnd);
        List<Integer> result = picked.subList(0, Math.min(count, picked.size()));
        Collections.sort(result);
        return result;
    }

    /** 生成1-max的随机池 */
    private List<Integer> randomPool(int min, int max) {
        List<Integer> pool = new ArrayList<>();
        for (int i = min; i <= max; i++) pool.add(i);
        return pool;
    }

    /** 加权随机 */
    private int weightedRandom(double[] weights, Random rnd) {
        double total = Arrays.stream(weights).sum();
        double r = rnd.nextDouble() * total;
        double cumulative = 0;
        for (int i = 0; i < weights.length; i++) {
            cumulative += weights[i];
            if (r <= cumulative) return i;
        }
        return weights.length - 1;
    }

    private List<LotteryResult> tail(List<LotteryResult> list, int n) {
        return list.size() <= n ? list : list.subList(list.size() - n, list.size());
    }

    // ============================================================
    //  频率/遗漏/统计 (委托 StatisticsUtils)
    // ============================================================

    private int[] calcFreq(List<LotteryResult> rows, boolean isRed, int max) {
        return StatisticsUtils.calcSsqRedFreq(rows, max);
    }

    private int[] calcFreqSsqBlue(List<LotteryResult> rows) {
        return StatisticsUtils.calcSsqBlueFreq(rows);
    }

    private int[] calcFreqDltFront(List<LotteryResult> rows) {
        return StatisticsUtils.calcDltFrontFreq(rows);
    }

    private int[] calcFreqDltBack(List<LotteryResult> rows) {
        return StatisticsUtils.calcDltBackFreq(rows);
    }

    private int[] calcFreqQlc(List<LotteryResult> rows) {
        return StatisticsUtils.calcQlcFreq(rows);
    }

    private int[] calcMissing(List<LotteryResult> rows, boolean isRed, int max) {
        return StatisticsUtils.calcMissing(rows, isRed, max);
    }

    private int[] calcMissingSsqBlue(List<LotteryResult> rows) {
        return StatisticsUtils.calcSsqBlueMissing(rows);
    }

    private int[] calcMissingQlc(List<LotteryResult> rows) {
        return StatisticsUtils.calcQlcMissing(rows);
    }

    private List<Integer> topNIndices(int[] freq, int min, int max, int n, boolean hot) {
        return StatisticsUtils.topNIndices(freq, min, max, n, hot);
    }

    private int calcAC(List<Integer> nums) {
        return StatisticsUtils.calcAC(nums);
    }
}
