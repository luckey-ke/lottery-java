package com.lottery.service;

import com.lottery.common.LotteryNumberUtils;
import com.lottery.common.LotteryNumberUtils.ParsedDlt;
import com.lottery.common.LotteryNumberUtils.ParsedSsq;
import com.lottery.common.StatisticsUtils;
import com.lottery.entity.LotteryResult;
import com.lottery.entity.LotteryType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private static final Set<Integer> PRIMES = Set.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31);

    private final LotteryResultService resultService;

    // ============================================================
    //  对外接口
    // ============================================================

    public Map<String, Object> analyze(String type) {
        List<LotteryResult> rows = resultService.allNumbers(type);
        if (rows.isEmpty()) return Map.of("error", "[" + type + "] 暂无数据");

        return switch (type) {
            case "ssq" -> analyzeSsq(rows);
            case "dlt" -> analyzeDlt(rows);
            case "fc3d", "pl3" -> analyzePositional(rows, type, 3);
            case "pl5" -> analyzePositional(rows, type, 5);
            case "qlc" -> analyzeQlc(rows);
            default -> Map.of("error", "未知彩种: " + type);
        };
    }

    public Map<String, Object> analyzeAll() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (LotteryType lt : LotteryType.values()) {
            result.put(lt.getCode(), analyze(lt.getCode()));
        }
        return result;
    }

    public Map<String, Object> trend(String type, int recentN) {
        List<LotteryResult> all = resultService.allNumbers(type);
        if (all.isEmpty()) return Map.of("error", "暂无数据");

        List<LotteryResult> recent = all.size() > recentN
                ? all.subList(all.size() - recentN, all.size()) : all;
        return switch (type) {
            case "ssq" -> trendSsq(recent);
            case "dlt" -> trendDlt(recent);
            case "fc3d", "pl3" -> trendPositional(recent, type, 3);
            case "pl5" -> trendPositional(recent, type, 5);
            case "qlc" -> trendQlc(recent);
            default -> Map.of("info", "[" + type + "] 趋势分析暂不支持");
        };
    }

    // ============================================================
    //  双色球分析
    // ============================================================

    private Map<String, Object> analyzeSsq(List<LotteryResult> rows) {
        int[] redFreq = new int[34]; // 1-33
        int[] blueFreq = new int[17]; // 1-16
        List<Integer> sums = new ArrayList<>();
        List<Integer> spans = new ArrayList<>();
        List<Integer> acValues = new ArrayList<>();
        List<Integer> consecutiveCounts = new ArrayList<>();
        List<Integer> sumTails = new ArrayList<>();
        Map<String, Integer> oddEven = new LinkedHashMap<>();
        Map<String, Integer> sizeRatio = new LinkedHashMap<>();
        Map<String, Integer> primeComposite = new LinkedHashMap<>();
        Map<String, Integer> mod012 = new LinkedHashMap<>();
        Map<String, Integer> zoneRatio3 = new LinkedHashMap<>();
        Map<Integer, Integer> consecutiveDist = new LinkedHashMap<>();
        Map<Integer, Integer> sumTailDist = new LinkedHashMap<>();
        Map<Integer, Integer> spanDist = new LinkedHashMap<>();
        Map<Integer, Integer> acDist = new LinkedHashMap<>();
        int totalRepeatCount = 0;

        ParsedSsq prev = null;
        for (int idx = 0; idx < rows.size(); idx++) {
            ParsedSsq p = LotteryNumberUtils.parseSsq(rows.get(idx).getNumbers());
            for (int r : p.reds()) redFreq[r]++;
            for (int b : p.blue()) blueFreq[b]++;

            int sum = Arrays.stream(p.reds()).sum();
            sums.add(sum);
            sumTails.add(sum % 10);
            sumTailDist.merge(sum % 10, 1, Integer::sum);

            int span = p.reds()[p.reds().length - 1] - p.reds()[0];
            spans.add(span);
            spanDist.merge(span, 1, Integer::sum);

            int ac = LotteryNumberUtils.calcAC(p.reds());
            acValues.add(ac);
            acDist.merge(ac, 1, Integer::sum);

            int consec = LotteryNumberUtils.countConsecutive(p.reds());
            consecutiveCounts.add(consec);
            consecutiveDist.merge(consec, 1, Integer::sum);

            long odds = Arrays.stream(p.reds()).filter(r -> r % 2 == 1).count();
            oddEven.merge(odds + ":" + (6 - odds), 1, Integer::sum);

            long bigs = Arrays.stream(p.reds()).filter(r -> r >= 17).count();
            sizeRatio.merge(bigs + ":" + (6 - bigs), 1, Integer::sum);

            long primes = Arrays.stream(p.reds()).filter(PRIMES::contains).count();
            primeComposite.merge(primes + ":" + (6 - primes), 1, Integer::sum);

            long mod0 = Arrays.stream(p.reds()).filter(r -> r % 3 == 0).count();
            long mod1 = Arrays.stream(p.reds()).filter(r -> r % 3 == 1).count();
            long mod2 = Arrays.stream(p.reds()).filter(r -> r % 3 == 2).count();
            mod012.merge(mod0 + ":" + mod1 + ":" + mod2, 1, Integer::sum);

            long z1 = Arrays.stream(p.reds()).filter(r -> r <= 11).count();
            long z2 = Arrays.stream(p.reds()).filter(r -> r >= 12 && r <= 22).count();
            long z3 = Arrays.stream(p.reds()).filter(r -> r >= 23).count();
            zoneRatio3.merge(z1 + ":" + z2 + ":" + z3, 1, Integer::sum);

            if (prev != null) {
                Set<Integer> prevSet = Arrays.stream(prev.reds()).boxed().collect(Collectors.toSet());
                int repeats = (int) Arrays.stream(p.reds()).filter(prevSet::contains).count();
                totalRepeatCount += repeats;
            }
            prev = p;
        }

        Map<String, Integer> redMissing = calcMissing(rows, true);
        Map<String, Integer> blueMissing = calcMissing(rows, false);

        double avgSum = sums.stream().mapToInt(Integer::intValue).average().orElse(0);
        double avgSpan = spans.stream().mapToInt(Integer::intValue).average().orElse(0);
        double avgAC = acValues.stream().mapToInt(Integer::intValue).average().orElse(0);
        double avgConsec = consecutiveCounts.stream().mapToInt(Integer::intValue).average().orElse(0);

        Map<String, Integer> redFreqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 33; i++) redFreqMap.put(String.format("%02d", i), redFreq[i]);
        Map<String, Integer> blueFreqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 16; i++) blueFreqMap.put(String.format("%02d", i), blueFreq[i]);

        Map<String, Object> result = new HashMap<>();
        result.put("lotteryType", "ssq");
        result.put("name", "双色球");
        result.put("totalDraws", rows.size());
        result.put("redFreq", redFreqMap);
        result.put("blueFreq", blueFreqMap);
        result.put("redHot", topN(redFreq, 1, 33, 10, true));
        result.put("redCold", topN(redFreq, 1, 33, 10, false));
        result.put("blueHot", topN(blueFreq, 1, 16, 5, true));
        result.put("blueCold", topN(blueFreq, 1, 16, 5, false));
        result.put("redMissing", redMissing);
        result.put("blueMissing", blueMissing);

        // 和值
        result.put("sumStats", buildStatsMap(sums));
        result.put("sumTails", sortIntMap(sumTailDist));

        // 跨度
        result.put("spanStats", buildStatsMap(spans));
        result.put("spanDistribution", sortIntMap(spanDist));

        // AC值
        result.put("acStats", Map.of("avg", round1(avgAC),
                "min", acValues.stream().mapToInt(Integer::intValue).min().orElse(0),
                "max", acValues.stream().mapToInt(Integer::intValue).max().orElse(0)));
        result.put("acDistribution", sortIntMap(acDist));

        // 连号
        result.put("consecutiveStats", Map.of("avg", round1(avgConsec)));
        result.put("consecutiveDistribution", sortIntMap(consecutiveDist));

        // 重号
        result.put("avgRepeats", rows.size() > 1 ? round1((double) totalRepeatCount / (rows.size() - 1)) : 0);

        // 奇偶/大小/质合
        result.put("oddEvenRatio", oddEven);
        result.put("sizeRatio", sizeRatio);
        result.put("primeCompositeRatio", primeComposite);

        // 012路
        result.put("mod012Ratio", mod012);

        // 三区比
        result.put("zoneRatio3", zoneRatio3);

        return result;
    }

    // ============================================================
    //  大乐透分析
    // ============================================================

    private Map<String, Object> analyzeDlt(List<LotteryResult> rows) {
        int[] frontFreq = new int[36]; // 1-35
        int[] backFreq = new int[13];  // 1-12
        List<Integer> frontSums = new ArrayList<>();
        List<Integer> frontSpans = new ArrayList<>();
        List<Integer> frontACs = new ArrayList<>();
        List<Integer> frontConsecs = new ArrayList<>();
        List<Integer> sumTails = new ArrayList<>();
        Map<String, Integer> oddEven = new LinkedHashMap<>();
        Map<String, Integer> sizeRatio = new LinkedHashMap<>();
        Map<String, Integer> primeComposite = new LinkedHashMap<>();
        Map<String, Integer> mod012 = new LinkedHashMap<>();
        Map<String, Integer> zoneRatio3 = new LinkedHashMap<>();
        Map<Integer, Integer> consecutiveDist = new LinkedHashMap<>();
        Map<Integer, Integer> spanDist = new LinkedHashMap<>();
        Map<Integer, Integer> acDist = new LinkedHashMap<>();
        Map<Integer, Integer> sumTailDist = new LinkedHashMap<>();
        int totalRepeatCount = 0;

        ParsedDlt prev = null;
        for (LotteryResult row : rows) {
            ParsedDlt p = LotteryNumberUtils.parseDlt(row.getNumbers());
            for (int f : p.front()) frontFreq[f]++;
            for (int b : p.back()) backFreq[b]++;

            int sum = Arrays.stream(p.front()).sum();
            frontSums.add(sum);
            sumTails.add(sum % 10);
            sumTailDist.merge(sum % 10, 1, Integer::sum);

            int span = p.front()[p.front().length - 1] - p.front()[0];
            frontSpans.add(span);
            spanDist.merge(span, 1, Integer::sum);

            int ac = LotteryNumberUtils.calcAC(p.front());
            frontACs.add(ac);
            acDist.merge(ac, 1, Integer::sum);

            int consec = LotteryNumberUtils.countConsecutive(p.front());
            frontConsecs.add(consec);
            consecutiveDist.merge(consec, 1, Integer::sum);

            long odds = Arrays.stream(p.front()).filter(f -> f % 2 == 1).count();
            oddEven.merge(odds + ":" + (5 - odds), 1, Integer::sum);

            long bigs = Arrays.stream(p.front()).filter(f -> f >= 18).count();
            sizeRatio.merge(bigs + ":" + (5 - bigs), 1, Integer::sum);

            long primes = Arrays.stream(p.front()).filter(PRIMES::contains).count();
            primeComposite.merge(primes + ":" + (5 - primes), 1, Integer::sum);

            long mod0 = Arrays.stream(p.front()).filter(f -> f % 3 == 0).count();
            long mod1 = Arrays.stream(p.front()).filter(f -> f % 3 == 1).count();
            long mod2 = Arrays.stream(p.front()).filter(f -> f % 3 == 2).count();
            mod012.merge(mod0 + ":" + mod1 + ":" + mod2, 1, Integer::sum);

            long z1 = Arrays.stream(p.front()).filter(f -> f <= 12).count();
            long z2 = Arrays.stream(p.front()).filter(f -> f >= 13 && f <= 24).count();
            long z3 = Arrays.stream(p.front()).filter(f -> f >= 25).count();
            zoneRatio3.merge(z1 + ":" + z2 + ":" + z3, 1, Integer::sum);

            if (prev != null) {
                Set<Integer> prevSet = Arrays.stream(prev.front()).boxed().collect(Collectors.toSet());
                int repeats = (int) Arrays.stream(p.front()).filter(prevSet::contains).count();
                totalRepeatCount += repeats;
            }
            prev = p;
        }

        Map<String, Integer> frontFreqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 35; i++) frontFreqMap.put(String.format("%02d", i), frontFreq[i]);
        Map<String, Integer> backFreqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) backFreqMap.put(String.format("%02d", i), backFreq[i]);

        Map<String, Object> result = new HashMap<>();
        result.put("lotteryType", "dlt");
        result.put("name", "大乐透");
        result.put("totalDraws", rows.size());
        result.put("frontFreq", frontFreqMap);
        result.put("backFreq", backFreqMap);
        result.put("frontHot", topN(frontFreq, 1, 35, 10, true));
        result.put("frontCold", topN(frontFreq, 1, 35, 10, false));
        result.put("backHot", topN(backFreq, 1, 12, 5, true));
        result.put("backCold", topN(backFreq, 1, 12, 5, false));

        result.put("sumStats", buildStatsMap(frontSums));
        result.put("sumTails", sortIntMap(sumTailDist));
        result.put("spanStats", buildStatsMap(frontSpans));
        result.put("spanDistribution", sortIntMap(spanDist));
        result.put("acStats", Map.of("avg", round1(frontACs.stream().mapToInt(Integer::intValue).average().orElse(0)),
                "min", frontACs.stream().mapToInt(Integer::intValue).min().orElse(0),
                "max", frontACs.stream().mapToInt(Integer::intValue).max().orElse(0)));
        result.put("acDistribution", sortIntMap(acDist));
        result.put("consecutiveStats", Map.of("avg", round1(frontConsecs.stream().mapToInt(Integer::intValue).average().orElse(0))));
        result.put("consecutiveDistribution", sortIntMap(consecutiveDist));
        result.put("avgRepeats", rows.size() > 1 ? round1((double) totalRepeatCount / (rows.size() - 1)) : 0);

        result.put("oddEvenRatio", oddEven);
        result.put("sizeRatio", sizeRatio);
        result.put("primeCompositeRatio", primeComposite);
        result.put("mod012Ratio", mod012);
        result.put("zoneRatio3", zoneRatio3);

        return result;
    }

    // ============================================================
    //  位置型分析（3D / 排列三 / 排列五）
    // ============================================================

    private Map<String, Object> analyzePositional(List<LotteryResult> rows, String type, int positions) {
        int[][] posFreq = new int[positions][10]; // 0-9
        Map<String, Integer> comboFreq = new LinkedHashMap<>();
        Map<Integer, Integer> sumFreq = new LinkedHashMap<>();
        List<Integer> spans = new ArrayList<>();
        List<Integer> sumTails = new ArrayList<>();
        Map<String, Integer> oddEven = new LinkedHashMap<>();
        Map<String, Integer> primeComposite = new LinkedHashMap<>();
        Map<Integer, Integer> spanDist = new LinkedHashMap<>();
        Map<Integer, Integer> sumTailDist = new LinkedHashMap<>();
        Map<String, Integer> mod012 = new LinkedHashMap<>();
        Map<Integer, Integer> consecutiveDist = new LinkedHashMap<>();
        int dragonCount = 0; // 龙: first > last
        int tigerCount = 0;  // 虎: first < last
        int drawCount = 0;   // 和: first == last
        int totalRepeatCount = 0;

        int[] prevNums = null;
        for (LotteryResult row : rows) {
            int[] nums = LotteryNumberUtils.parsePositional(row.getNumbers(), positions);
            for (int i = 0; i < positions; i++) posFreq[i][nums[i]]++;
            String combo = Arrays.toString(nums);
            comboFreq.merge(combo, 1, Integer::sum);
            int s = Arrays.stream(nums).sum();
            sumFreq.merge(s, 1, Integer::sum);
            sumTails.add(s % 10);
            sumTailDist.merge(s % 10, 1, Integer::sum);

            int span = Arrays.stream(nums).max().orElse(0) - Arrays.stream(nums).min().orElse(0);
            spans.add(span);
            spanDist.merge(span, 1, Integer::sum);

            long odds = Arrays.stream(nums).filter(n -> n % 2 == 1).count();
            oddEven.merge(odds + ":" + (positions - odds), 1, Integer::sum);

            long primes = Arrays.stream(nums).filter(n -> Set.of(2, 3, 5, 7).contains(n)).count();
            primeComposite.merge(primes + ":" + (positions - primes), 1, Integer::sum);

            long mod0 = Arrays.stream(nums).filter(n -> n % 3 == 0).count();
            long mod1 = Arrays.stream(nums).filter(n -> n % 3 == 1).count();
            long mod2 = Arrays.stream(nums).filter(n -> n % 3 == 2).count();
            mod012.merge(mod0 + ":" + mod1 + ":" + mod2, 1, Integer::sum);

            int consec = LotteryNumberUtils.countConsecutive(nums);
            consecutiveDist.merge(consec, 1, Integer::sum);

            // 龙虎和 (仅两位比较首尾)
            if (nums[0] > nums[positions - 1]) dragonCount++;
            else if (nums[0] < nums[positions - 1]) tigerCount++;
            else drawCount++;

            if (prevNums != null) {
                Set<Integer> prevSet = Arrays.stream(prevNums).boxed().collect(Collectors.toSet());
                totalRepeatCount += (int) Arrays.stream(nums).filter(prevSet::contains).count();
            }
            prevNums = nums;
        }

        List<Map<String, Object>> posAnalysis = new ArrayList<>();
        for (int i = 0; i < positions; i++) {
            Map<String, Object> pa = new LinkedHashMap<>();
            pa.put("position", i + 1);
            Map<String, Integer> freq = new LinkedHashMap<>();
            for (int j = 0; j < 10; j++) freq.put(String.valueOf(j), posFreq[i][j]);
            pa.put("freq", freq);
            pa.put("hot", topN10(posFreq[i], 3, true));
            pa.put("cold", topN10(posFreq[i], 3, false));
            posAnalysis.add(pa);
        }

        List<Map<String, Object>> topCombos = comboFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("combo", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                }).toList();

        Map<String, Integer> sortedSum = new LinkedHashMap<>();
        sumFreq.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> sortedSum.put(String.valueOf(e.getKey()), e.getValue()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lotteryType", type);
        result.put("name", LotteryType.fromCode(type).getName());
        result.put("totalDraws", rows.size());
        result.put("positions", posAnalysis);
        result.put("sumDistribution", sortedSum);
        result.put("sumStats", buildStatsMap(sumFreq.keySet().stream().toList()));
        result.put("sumTails", sortIntMap(sumTailDist));
        result.put("topCombos", topCombos);
        result.put("spanStats", buildStatsMap(spans));
        result.put("spanDistribution", sortIntMap(spanDist));
        result.put("oddEvenRatio", oddEven);
        result.put("primeCompositeRatio", primeComposite);
        result.put("mod012Ratio", mod012);
        result.put("consecutiveDistribution", sortIntMap(consecutiveDist));
        result.put("avgRepeats", rows.size() > 1 ? round1((double) totalRepeatCount / (rows.size() - 1)) : 0);
        result.put("dragonTiger", Map.of(
                "dragon", dragonCount,
                "tiger", tigerCount,
                "draw", drawCount,
                "dragonPct", round1((double) dragonCount / rows.size() * 100),
                "tigerPct", round1((double) tigerCount / rows.size() * 100)
        ));
        return result;
    }

    // ============================================================
    //  七乐彩分析
    // ============================================================

    private Map<String, Object> analyzeQlc(List<LotteryResult> rows) {
        int[] freq = new int[31]; // 1-30
        List<Integer> sums = new ArrayList<>();
        List<Integer> spans = new ArrayList<>();
        List<Integer> acValues = new ArrayList<>();
        List<Integer> consecutiveCounts = new ArrayList<>();
        List<Integer> sumTails = new ArrayList<>();
        Map<String, Integer> oddEven = new LinkedHashMap<>();
        Map<String, Integer> sizeRatio = new LinkedHashMap<>();
        Map<String, Integer> primeComposite = new LinkedHashMap<>();
        Map<String, Integer> mod012 = new LinkedHashMap<>();
        Map<String, Integer> zoneRatio3 = new LinkedHashMap<>();
        Map<Integer, Integer> consecutiveDist = new LinkedHashMap<>();
        Map<Integer, Integer> spanDist = new LinkedHashMap<>();
        Map<Integer, Integer> acDist = new LinkedHashMap<>();
        Map<Integer, Integer> sumTailDist = new LinkedHashMap<>();
        int totalRepeatCount = 0;

        int[] prevNums = null;
        for (LotteryResult row : rows) {
            int[] nums = LotteryNumberUtils.parseGeneric(row.getNumbers(), 7);
            Arrays.sort(nums);
            for (int n : nums) if (n >= 1 && n <= 30) freq[n]++;

            int sum = Arrays.stream(nums).sum();
            sums.add(sum);
            sumTails.add(sum % 10);
            sumTailDist.merge(sum % 10, 1, Integer::sum);

            int span = nums[nums.length - 1] - nums[0];
            spans.add(span);
            spanDist.merge(span, 1, Integer::sum);

            int ac = LotteryNumberUtils.calcAC(nums);
            acValues.add(ac);
            acDist.merge(ac, 1, Integer::sum);

            int consec = LotteryNumberUtils.countConsecutive(nums);
            consecutiveCounts.add(consec);
            consecutiveDist.merge(consec, 1, Integer::sum);

            long odds = Arrays.stream(nums).filter(n -> n % 2 == 1).count();
            oddEven.merge(odds + ":" + (7 - odds), 1, Integer::sum);

            long bigs = Arrays.stream(nums).filter(n -> n >= 16).count();
            sizeRatio.merge(bigs + ":" + (7 - bigs), 1, Integer::sum);

            long primes = Arrays.stream(nums).filter(n -> n <= 30 && PRIMES.contains(n)).count();
            primeComposite.merge(primes + ":" + (7 - primes), 1, Integer::sum);

            long mod0 = Arrays.stream(nums).filter(n -> n % 3 == 0).count();
            long mod1 = Arrays.stream(nums).filter(n -> n % 3 == 1).count();
            long mod2 = Arrays.stream(nums).filter(n -> n % 3 == 2).count();
            mod012.merge(mod0 + ":" + mod1 + ":" + mod2, 1, Integer::sum);

            long z1 = Arrays.stream(nums).filter(n -> n <= 10).count();
            long z2 = Arrays.stream(nums).filter(n -> n >= 11 && n <= 20).count();
            long z3 = Arrays.stream(nums).filter(n -> n >= 21).count();
            zoneRatio3.merge(z1 + ":" + z2 + ":" + z3, 1, Integer::sum);

            if (prevNums != null) {
                Set<Integer> prevSet = Arrays.stream(prevNums).boxed().collect(Collectors.toSet());
                totalRepeatCount += (int) Arrays.stream(nums).filter(prevSet::contains).count();
            }
            prevNums = nums;
        }

        Map<String, Integer> freqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 30; i++) freqMap.put(String.format("%02d", i), freq[i]);

        Map<String, Object> result = new HashMap<>();
        result.put("lotteryType", "qlc");
        result.put("name", "七乐彩");
        result.put("totalDraws", rows.size());
        result.put("freq", freqMap);
        result.put("hot", topN(freq, 1, 30, 10, true));
        result.put("cold", topN(freq, 1, 30, 10, false));
        result.put("sumStats", buildStatsMap(sums));
        result.put("sumTails", sortIntMap(sumTailDist));
        result.put("spanStats", buildStatsMap(spans));
        result.put("spanDistribution", sortIntMap(spanDist));
        result.put("acStats", Map.of("avg", round1(acValues.stream().mapToInt(Integer::intValue).average().orElse(0)),
                "min", acValues.stream().mapToInt(Integer::intValue).min().orElse(0),
                "max", acValues.stream().mapToInt(Integer::intValue).max().orElse(0)));
        result.put("acDistribution", sortIntMap(acDist));
        result.put("consecutiveStats", Map.of("avg", round1(consecutiveCounts.stream().mapToInt(Integer::intValue).average().orElse(0))));
        result.put("consecutiveDistribution", sortIntMap(consecutiveDist));
        result.put("avgRepeats", rows.size() > 1 ? round1((double) totalRepeatCount / (rows.size() - 1)) : 0);
        result.put("oddEvenRatio", oddEven);
        result.put("sizeRatio", sizeRatio);
        result.put("primeCompositeRatio", primeComposite);
        result.put("mod012Ratio", mod012);
        result.put("zoneRatio3", zoneRatio3);
        return result;
    }

    // ============================================================
    //  趋势分析
    // ============================================================

    private Map<String, Object> trendSsq(List<LotteryResult> rows) {
        List<Map<String, Object>> trend = new ArrayList<>();
        ParsedSsq prev = null;
        for (LotteryResult row : rows) {
            ParsedSsq p = LotteryNumberUtils.parseSsq(row.getNumbers());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("drawNum", row.getDrawNum());
            m.put("drawDate", row.getDrawDate());
            m.put("numbers", row.getNumbers());
            m.put("sum", Arrays.stream(p.reds()).sum());
            m.put("span", p.reds()[p.reds().length - 1] - p.reds()[0]);
            m.put("ac", LotteryNumberUtils.calcAC(p.reds()));
            m.put("consecutive", LotteryNumberUtils.countConsecutive(p.reds()));
            m.put("oddCount", (int) Arrays.stream(p.reds()).filter(r -> r % 2 == 1).count());
            m.put("bigCount", (int) Arrays.stream(p.reds()).filter(r -> r >= 17).count());
            m.put("primeCount", (int) Arrays.stream(p.reds()).filter(PRIMES::contains).count());
            m.put("zone1", (int) Arrays.stream(p.reds()).filter(r -> r <= 11).count());
            m.put("zone2", (int) Arrays.stream(p.reds()).filter(r -> r >= 12 && r <= 22).count());
            m.put("zone3", (int) Arrays.stream(p.reds()).filter(r -> r >= 23).count());
            m.put("blue", p.blue().length > 0 ? p.blue()[0] : 0);
            if (prev != null) {
                Set<Integer> prevSet = Arrays.stream(prev.reds()).boxed().collect(Collectors.toSet());
                m.put("repeats", (int) Arrays.stream(p.reds()).filter(prevSet::contains).count());
            }
            trend.add(m);
            prev = p;
        }
        return Map.of("lotteryType", "ssq", "trend", trend);
    }

    private Map<String, Object> trendDlt(List<LotteryResult> rows) {
        List<Map<String, Object>> trend = new ArrayList<>();
        ParsedDlt prev = null;
        for (LotteryResult row : rows) {
            ParsedDlt p = LotteryNumberUtils.parseDlt(row.getNumbers());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("drawNum", row.getDrawNum());
            m.put("drawDate", row.getDrawDate());
            m.put("numbers", row.getNumbers());
            m.put("frontSum", Arrays.stream(p.front()).sum());
            m.put("frontSpan", p.front()[p.front().length - 1] - p.front()[0]);
            m.put("frontAC", LotteryNumberUtils.calcAC(p.front()));
            m.put("frontOdd", (int) Arrays.stream(p.front()).filter(f -> f % 2 == 1).count());
            m.put("frontPrime", (int) Arrays.stream(p.front()).filter(PRIMES::contains).count());
            m.put("backSum", Arrays.stream(p.back()).sum());
            if (prev != null) {
                Set<Integer> prevSet = Arrays.stream(prev.front()).boxed().collect(Collectors.toSet());
                m.put("repeats", (int) Arrays.stream(p.front()).filter(prevSet::contains).count());
            }
            trend.add(m);
            prev = p;
        }
        return Map.of("lotteryType", "dlt", "trend", trend);
    }

    private Map<String, Object> trendPositional(List<LotteryResult> rows, String type, int positions) {
        List<Map<String, Object>> trend = new ArrayList<>();
        int[] prevNums = null;
        for (LotteryResult row : rows) {
            int[] nums = LotteryNumberUtils.parsePositional(row.getNumbers(), positions);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("drawNum", row.getDrawNum());
            m.put("drawDate", row.getDrawDate());
            m.put("numbers", row.getNumbers());
            m.put("sum", Arrays.stream(nums).sum());
            m.put("span", Arrays.stream(nums).max().orElse(0) - Arrays.stream(nums).min().orElse(0));
            m.put("oddCount", (int) Arrays.stream(nums).filter(n -> n % 2 == 1).count());
            m.put("bigCount", (int) Arrays.stream(nums).filter(n -> n >= 5).count());
            m.put("positions", Arrays.stream(nums).boxed().toList());
            if (prevNums != null) {
                Set<Integer> prevSet = Arrays.stream(prevNums).boxed().collect(Collectors.toSet());
                m.put("repeats", (int) Arrays.stream(nums).filter(prevSet::contains).count());
            }
            trend.add(m);
            prevNums = nums;
        }
        return Map.of("lotteryType", type, "trend", trend);
    }

    private Map<String, Object> trendQlc(List<LotteryResult> rows) {
        List<Map<String, Object>> trend = new ArrayList<>();
        int[] prevNums = null;
        for (LotteryResult row : rows) {
            int[] nums = LotteryNumberUtils.parseGeneric(row.getNumbers(), 7);
            Arrays.sort(nums);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("drawNum", row.getDrawNum());
            m.put("drawDate", row.getDrawDate());
            m.put("numbers", row.getNumbers());
            m.put("sum", Arrays.stream(nums).sum());
            m.put("span", nums[nums.length - 1] - nums[0]);
            m.put("ac", LotteryNumberUtils.calcAC(nums));
            m.put("oddCount", (int) Arrays.stream(nums).filter(n -> n % 2 == 1).count());
            m.put("bigCount", (int) Arrays.stream(nums).filter(n -> n >= 16).count());
            m.put("primeCount", (int) Arrays.stream(nums).filter(n -> n <= 30 && PRIMES.contains(n)).count());
            if (prevNums != null) {
                Set<Integer> prevSet = Arrays.stream(prevNums).boxed().collect(Collectors.toSet());
                m.put("repeats", (int) Arrays.stream(nums).filter(prevSet::contains).count());
            }
            trend.add(m);
            prevNums = nums;
        }
        return Map.of("lotteryType", "qlc", "trend", trend);
    }

    // ============================================================
    //  号码解析（委托 LotteryNumberUtils）
    // ============================================================

    // ============================================================
    //  统计工具 (委托 StatisticsUtils)
    // ============================================================

    private Map<String, Integer> calcMissing(List<LotteryResult> rows, boolean isRed) {
        int max = isRed ? 33 : 16;
        int[] miss;
        if (isRed) {
            miss = StatisticsUtils.calcSsqRedMissing(rows);
        } else {
            miss = StatisticsUtils.calcSsqBlueMissing(rows);
        }
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 1; i <= max; i++) map.put(String.format("%02d", i), miss[i]);
        return map;
    }

    private Map<String, Object> buildStatsMap(List<Integer> values) {
        if (values.isEmpty()) return Map.of("avg", 0, "min", 0, "max", 0);
        IntSummaryStatistics stats = values.stream().mapToInt(Integer::intValue).summaryStatistics();
        return Map.of("avg", round1(stats.getAverage()),
                "min", stats.getMin(),
                "max", stats.getMax());
    }

    private Map<String, Object> buildStatsMap(IntSummaryStatistics stats) {
        return Map.of("avg", round1(stats.getAverage()),
                "min", stats.getMin(),
                "max", stats.getMax());
    }

    private double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private Map<String, Integer> sortIntMap(Map<Integer, Integer> map) {
        Map<String, Integer> result = new LinkedHashMap<>();
        map.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> result.put(String.valueOf(e.getKey()), e.getValue()));
        return result;
    }

    private List<String> topN(int[] freq, int min, int max, int n, boolean hot) {
        return StatisticsUtils.topN(freq, min, max, n, hot);
    }

    private List<String> topN10(int[] freq, int n, boolean hot) {
        return StatisticsUtils.topN10(freq, n, hot);
    }
}
