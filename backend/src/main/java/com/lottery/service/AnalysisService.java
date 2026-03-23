package com.lottery.service;

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
            default -> Map.of("info", "[" + type + "] 趋势分析暂仅支持双色球和大乐透");
        };
    }

    // ============================================================
    //  双色球分析
    // ============================================================

    private Map<String, Object> analyzeSsq(List<LotteryResult> rows) {
        int[] redFreq = new int[34]; // 1-33
        int[] blueFreq = new int[17]; // 1-16
        List<Integer> sums = new ArrayList<>();
        Map<String, Integer> oddEven = new LinkedHashMap<>();
        Map<String, Integer> sizeRatio = new LinkedHashMap<>();

        for (LotteryResult row : rows) {
            ParsedSsq p = parseSsq(row.getNumbers());
            for (int r : p.reds) redFreq[r]++;
            for (int b : p.blue) blueFreq[b]++;
            int s = Arrays.stream(p.reds).sum();
            sums.add(s);
            long odds = Arrays.stream(p.reds).filter(r -> r % 2 == 1).count();
            oddEven.merge(odds + ":" + (6 - odds), 1, Integer::sum);
            long bigs = Arrays.stream(p.reds).filter(r -> r >= 17).count();
            sizeRatio.merge(bigs + ":" + (6 - bigs), 1, Integer::sum);
        }

        Map<String, Integer> redMissing = calcMissingSsq(rows, true);
        Map<String, Integer> blueMissing = calcMissingSsq(rows, false);

        double avg = sums.stream().mapToInt(Integer::intValue).average().orElse(0);

        // Build freq maps
        Map<String, Integer> redFreqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 33; i++) redFreqMap.put(String.format("%02d", i), redFreq[i]);
        Map<String, Integer> blueFreqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 16; i++) blueFreqMap.put(String.format("%02d", i), blueFreq[i]);

        // Hot / Cold
        List<String> redHot = topN(redFreq, 1, 33, 10, true);
        List<String> redCold = topN(redFreq, 1, 33, 10, false);
        List<String> blueHot = topN(blueFreq, 1, 16, 5, true);
        List<String> blueCold = topN(blueFreq, 1, 16, 5, false);


        Map<String, Object> result = new HashMap<>();
        result.put("lotteryType", "ssq");
        result.put("name", "双色球");
        result.put("totalDraws", rows.size());
        result.put("redFreq", redFreqMap);
        result.put("blueFreq", blueFreqMap);
        result.put("redHot", redHot);
        result.put("redCold", redCold);
        result.put("blueHot", blueHot);
        result.put("blueCold", blueCold);
        result.put("redMissing", redMissing);
        result.put("blueMissing", blueMissing);
        result.put("sumStats", Map.of("avg", Math.round(avg * 10) / 10.0,
                "min", sums.stream().mapToInt(Integer::intValue).min().orElse(0),
                "max", sums.stream().mapToInt(Integer::intValue).max().orElse(0)));
        result.put("oddEvenRatio", oddEven);
        result.put("sizeRatio", sizeRatio);
        return result;
    }


    // ============================================================
    //  大乐透分析
    // ============================================================

    private Map<String, Object> analyzeDlt(List<LotteryResult> rows) {
        int[] frontFreq = new int[36]; // 1-35
        int[] backFreq = new int[13];  // 1-12
        List<Integer> sums = new ArrayList<>();

        for (LotteryResult row : rows) {
            ParsedDlt p = parseDlt(row.getNumbers());
            for (int f : p.front) frontFreq[f]++;
            for (int b : p.back) backFreq[b]++;
            sums.add(Arrays.stream(p.front).sum());
        }

        double avg = sums.stream().mapToInt(Integer::intValue).average().orElse(0);

        Map<String, Integer> frontFreqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 35; i++) frontFreqMap.put(String.format("%02d", i), frontFreq[i]);
        Map<String, Integer> backFreqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) backFreqMap.put(String.format("%02d", i), backFreq[i]);

        return Map.of(
                "lotteryType", "dlt", "name", "大乐透", "totalDraws", rows.size(),
                "frontFreq", frontFreqMap, "backFreq", backFreqMap,
                "frontHot", topN(frontFreq, 1, 35, 10, true),
                "frontCold", topN(frontFreq, 1, 35, 10, false),
                "backHot", topN(backFreq, 1, 12, 5, true),
                "backCold", topN(backFreq, 1, 12, 5, false),
                "sumStats", Map.of("avg", Math.round(avg * 10) / 10.0,
                        "min", sums.stream().mapToInt(Integer::intValue).min().orElse(0),
                        "max", sums.stream().mapToInt(Integer::intValue).max().orElse(0))
        );
    }

    // ============================================================
    //  位置型分析（3D / 排列三 / 排列五）
    // ============================================================

    private Map<String, Object> analyzePositional(List<LotteryResult> rows, String type, int positions) {
        int[][] posFreq = new int[positions][10]; // 0-9
        Map<String, Integer> comboFreq = new LinkedHashMap<>();
        Map<Integer, Integer> sumFreq = new LinkedHashMap<>();

        for (LotteryResult row : rows) {
            int[] nums = parsePositional(row.getNumbers(), positions);
            for (int i = 0; i < positions; i++) posFreq[i][nums[i]]++;
            String combo = Arrays.toString(nums);
            comboFreq.merge(combo, 1, Integer::sum);
            int s = Arrays.stream(nums).sum();
            sumFreq.merge(s, 1, Integer::sum);
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

        // Top 20 combos
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

        return Map.of(
                "lotteryType", type, "name", LotteryType.fromCode(type).getName(),
                "totalDraws", rows.size(), "positions", posAnalysis,
                "sumDistribution", sortedSum, "topCombos", topCombos
        );
    }

    // ============================================================
    //  七乐彩分析
    // ============================================================

    private Map<String, Object> analyzeQlc(List<LotteryResult> rows) {
        int[] freq = new int[31]; // 1-30
        List<Integer> sums = new ArrayList<>();

        for (LotteryResult row : rows) {
            int[] nums = parseGeneric(row.getNumbers(), 7);
            for (int n : nums) if (n >= 1 && n <= 30) freq[n]++;
            sums.add(Arrays.stream(nums).sum());
        }

        double avg = sums.stream().mapToInt(Integer::intValue).average().orElse(0);

        Map<String, Integer> freqMap = new LinkedHashMap<>();
        for (int i = 1; i <= 30; i++) freqMap.put(String.format("%02d", i), freq[i]);

        return Map.of(
                "lotteryType", "qlc", "name", "七乐彩", "totalDraws", rows.size(),
                "freq", freqMap,
                "hot", topN(freq, 1, 30, 10, true),
                "cold", topN(freq, 1, 30, 10, false),
                "sumStats", Map.of("avg", Math.round(avg * 10) / 10.0)
        );
    }

    // ============================================================
    //  趋势分析
    // ============================================================

    private Map<String, Object> trendSsq(List<LotteryResult> rows) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (LotteryResult row : rows) {
            ParsedSsq p = parseSsq(row.getNumbers());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("drawNum", row.getDrawNum());
            m.put("drawDate", row.getDrawDate());
            m.put("sum", Arrays.stream(p.reds).sum());
            m.put("oddCount", (int) Arrays.stream(p.reds).filter(r -> r % 2 == 1).count());
            m.put("bigCount", (int) Arrays.stream(p.reds).filter(r -> r >= 17).count());
            m.put("zone1", (int) Arrays.stream(p.reds).filter(r -> r <= 11).count());
            m.put("zone2", (int) Arrays.stream(p.reds).filter(r -> r >= 12 && r <= 22).count());
            m.put("zone3", (int) Arrays.stream(p.reds).filter(r -> r >= 23).count());
            m.put("blue", p.blue.length > 0 ? p.blue[0] : 0);
            trend.add(m);
        }
        return Map.of("lotteryType", "ssq", "trend", trend);
    }

    private Map<String, Object> trendDlt(List<LotteryResult> rows) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (LotteryResult row : rows) {
            ParsedDlt p = parseDlt(row.getNumbers());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("drawNum", row.getDrawNum());
            m.put("drawDate", row.getDrawDate());
            m.put("frontSum", Arrays.stream(p.front).sum());
            m.put("frontOdd", (int) Arrays.stream(p.front).filter(f -> f % 2 == 1).count());
            m.put("backSum", Arrays.stream(p.back).sum());
            trend.add(m);
        }
        return Map.of("lotteryType", "dlt", "trend", trend);
    }

    // ============================================================
    //  号码解析
    // ============================================================

    private record ParsedSsq(int[] reds, int[] blue) {}

    private ParsedSsq parseSsq(String numbers) {
        String[] parts = numbers.replace(" ", "").split("\\+");
        int[] reds = Arrays.stream(parts[0].split(","))
                .filter(s -> !s.isBlank()).mapToInt(Integer::parseInt).toArray();
        int[] blue = parts.length > 1
                ? Arrays.stream(parts[1].split(",")).filter(s -> !s.isBlank()).mapToInt(Integer::parseInt).toArray()
                : new int[0];
        return new ParsedSsq(reds, blue);
    }

    private record ParsedDlt(int[] front, int[] back) {}

    private ParsedDlt parseDlt(String numbers) {
        String[] parts = numbers.replace(" ", "").split("\\+");
        int[] front = Arrays.stream(parts[0].split(","))
                .filter(s -> !s.isBlank()).mapToInt(Integer::parseInt).toArray();
        int[] back = parts.length > 1
                ? Arrays.stream(parts[1].split(",")).filter(s -> !s.isBlank()).mapToInt(Integer::parseInt).toArray()
                : new int[0];
        return new ParsedDlt(front, back);
    }

    private int[] parsePositional(String numbers, int count) {
        String cleaned = numbers.replace(",", "").replace(" ", "");
        int[] result = new int[count];
        for (int i = 0; i < count && i < cleaned.length(); i++) {
            result[i] = Character.getNumericValue(cleaned.charAt(i));
        }
        return result;
    }

    private int[] parseGeneric(String numbers, int count) {
        return Arrays.stream(numbers.replace(",", " ").trim().split("\\s+"))
                .filter(s -> !s.isBlank() && s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .limit(count)
                .toArray();
    }

    // ============================================================
    //  遗漏值
    // ============================================================

    private Map<String, Integer> calcMissingSsq(List<LotteryResult> rows, boolean isRed) {
        int max = isRed ? 33 : 16;
        int[] miss = new int[max + 1];
        for (LotteryResult row : rows) {
            ParsedSsq p = parseSsq(row.getNumbers());
            int[] appeared = isRed ? p.reds : p.blue;
            Set<Integer> set = Arrays.stream(appeared).boxed().collect(Collectors.toSet());
            for (int n = 1; n <= max; n++) {
                if (set.contains(n)) miss[n] = 0;
                else miss[n]++;
            }
        }
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 1; i <= max; i++) map.put(String.format("%02d", i), miss[i]);
        return map;
    }

    // ============================================================
    //  工具方法
    // ============================================================

    private List<String> topN(int[] freq, int min, int max, int n, boolean hot) {
        List<int[]> items = new ArrayList<>();
        for (int i = min; i <= max; i++) items.add(new int[]{i, freq[i]});
        items.sort(hot
                ? (a, b) -> Integer.compare(b[1], a[1])
                : (a, b) -> Integer.compare(a[1], b[1]));
        return items.subList(0, Math.min(n, items.size())).stream()
                .map(a -> String.format("%02d", a[0])).toList();
    }

    private List<String> topN10(int[] freq, int n, boolean hot) {
        List<int[]> items = new ArrayList<>();
        for (int i = 0; i < 10; i++) items.add(new int[]{i, freq[i]});
        items.sort(hot
                ? (a, b) -> Integer.compare(b[1], a[1])
                : (a, b) -> Integer.compare(a[1], b[1]));
        return items.subList(0, Math.min(n, items.size())).stream()
                .map(a -> String.valueOf(a[0])).toList();
    }
}
