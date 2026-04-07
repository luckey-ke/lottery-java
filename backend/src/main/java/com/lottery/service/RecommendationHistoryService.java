package com.lottery.service;

import com.lottery.entity.LotteryResult;
import com.lottery.entity.RecommendationHistory;
import com.lottery.mapper.RecommendationHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationHistoryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RecommendationHistoryMapper mapper;
    private final LotteryResultService resultService;

    /** 保存一组推荐记录 */
    public void save(String type, String date, List<Map<String, Object>> groups) {
        String now = LocalDateTime.now().format(TS_FMT);
        for (int i = 0; i < groups.size(); i++) {
            Map<String, Object> g = groups.get(i);
            RecommendationHistory rec = new RecommendationHistory();
            rec.setLotteryType(type);
            rec.setRecommendDate(date);
            rec.setStrategyName((String) g.get("strategy"));
            rec.setStrategyIndex(i);
            rec.setRecommendedNumbers((String) g.get("display"));
            rec.setHitMain(0);
            rec.setHitExtra(0);
            rec.setCreatedAt(now);
            rec.setUpdatedAt(now);
            try {
                mapper.upsert(rec);
            } catch (Exception e) {
                log.warn("保存推荐记录失败: {}", e.getMessage());
            }
        }
    }

    /** 查询某日推荐 */
    public List<RecommendationHistory> getByDate(String type, String date) {
        return mapper.findByDate(type, date);
    }

    /** 分页查询推荐日期列表 */
    public Map<String, Object> listHistory(String type, int limit, int offset) {
        List<String> dates = mapper.listDates(type, limit, offset);
        int total = mapper.countDates(type);

        List<Map<String, Object>> items = new ArrayList<>();
        for (String date : dates) {
            List<RecommendationHistory> records = mapper.findByTypeAndDate(type, date);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date);
            item.put("groups", records.stream().map(this::toMap).toList());
            items.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data", items);
        result.put("total", total);
        return result;
    }

    /** 获取命中率统计 */
    public Map<String, Object> getStats(String type) {
        List<Map<String, Object>> rawStats = mapper.getHitStats(type);
        List<Map<String, Object>> stats = new ArrayList<>();
        int totalDays = 0;

        for (Map<String, Object> row : rawStats) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("strategy", row.get("strategy_name"));
            int total = ((Number) row.get("total")).intValue();
            int hitCount = ((Number) row.get("hit_count")).intValue();
            s.put("total", total);
            s.put("hitCount", hitCount);
            s.put("hitRate", total > 0 ? Math.round((double) hitCount / total * 1000) / 10.0 : 0);
            s.put("avgHitMain", Math.round(((Number) row.get("avg_hit_main")).doubleValue() * 10) / 10.0);
            s.put("maxHitMain", row.get("max_hit_main"));
            s.put("avgHitExtra", Math.round(((Number) row.get("avg_hit_extra")).doubleValue() * 10) / 10.0);
            s.put("maxHitExtra", row.get("max_hit_extra"));
            stats.add(s);
            totalDays = Math.max(totalDays, total);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stats", stats);
        result.put("totalDays", totalDays);
        result.put("lotteryType", type);
        return result;
    }

    /** 自动匹配未对账的推荐记录与开奖结果 */
    public int autoMatch(String type) {
        List<RecommendationHistory> unmatched = mapper.findUnmatched(type);
        if (unmatched.isEmpty()) return 0;

        // 按日期分组
        Map<String, List<RecommendationHistory>> byDate = unmatched.stream()
                .collect(Collectors.groupingBy(RecommendationHistory::getRecommendDate));

        // 获取所有结果用于匹配
        List<LotteryResult> allResults = resultService.allNumbers(type);
        Map<String, LotteryResult> resultMap = new LinkedHashMap<>();
        for (LotteryResult r : allResults) {
            resultMap.put(r.getDrawDate(), r);
        }

        int matched = 0;
        String now = LocalDateTime.now().format(TS_FMT);

        for (Map.Entry<String, List<RecommendationHistory>> entry : byDate.entrySet()) {
            String date = entry.getKey();
            LotteryResult result = resultMap.get(date);
            if (result == null) continue;

            for (RecommendationHistory rec : entry.getValue()) {
                int[] hits = calculateHits(type, rec.getRecommendedNumbers(), result.getNumbers());
                rec.setActualNumbers(result.getNumbers());
                rec.setHitMain(hits[0]);
                rec.setHitExtra(hits[1]);
                rec.setUpdatedAt(now);
                try {
                    mapper.upsert(rec);
                    matched++;
                } catch (Exception e) {
                    log.warn("更新匹配结果失败: {}", e.getMessage());
                }
            }
        }
        return matched;
    }

    /** 根据历史命中率计算策略权重（返回5个权重值） */
    public double[] getStrategyWeights(String type) {
        // 默认权重
        double[] weights = {1.0, 1.0, 1.0, 1.0, 1.0};

        try {
            List<Map<String, Object>> stats = mapper.getHitStats(type);
            if (stats.isEmpty()) return weights;

            // 找出每个策略的命中率
            Map<String, Double> hitRates = new LinkedHashMap<>();
            for (Map<String, Object> row : stats) {
                String name = (String) row.get("strategy_name");
                int total = ((Number) row.get("total")).intValue();
                int hitCount = ((Number) row.get("hit_count")).intValue();
                double avgMain = ((Number) row.get("avg_hit_main")).doubleValue();
                // 综合评分：命中率 * 0.4 + 平均命中数 * 0.6
                double score = (total > 0 ? (double) hitCount / total : 0) * 0.4 + avgMain * 0.6;
                hitRates.put(name, score);
            }

            String[] strategies = {"均衡策略", "热号追踪", "冷号回补", "统计最优", "随机精选"};
            double maxScore = hitRates.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
            if (maxScore <= 0) maxScore = 1.0;

            for (int i = 0; i < strategies.length; i++) {
                Double score = hitRates.get(strategies[i]);
                if (score != null) {
                    // 归一化到 0.5 ~ 1.5 范围
                    weights[i] = 0.5 + (score / maxScore);
                }
            }

            log.info("[{}] 动态权重: {}", type, Arrays.toString(weights));
        } catch (Exception e) {
            log.warn("获取策略权重失败: {}", e.getMessage());
        }

        return weights;
    }

    // ============================================================
    //  命中计算
    // ============================================================

    private int[] calculateHits(String type, String recommended, String actual) {
        return switch (type) {
            case "ssq" -> calculateHitsSsq(recommended, actual);
            case "dlt" -> calculateHitsDlt(recommended, actual);
            case "fc3d", "pl3", "pl5" -> calculateHitsPositional(recommended, actual);
            case "qlc" -> calculateHitsQlc(recommended, actual);
            default -> new int[]{0, 0};
        };
    }

    private int[] calculateHitsSsq(String rec, String actual) {
        // rec format: "01 05 12 23 28 33 + 07"
        // actual format: "01,05,12,18,23,33+07"
        String[] recParts = rec.replace(" ", "").split("\\+");
        String[] actParts = actual.replace(" ", "").split("\\+");

        Set<String> recReds = parseBallSet(recParts[0]);
        Set<String> actReds = parseBallSet(actParts[0]);
        int hitMain = (int) recReds.stream().filter(actReds::contains).count();

        int hitExtra = 0;
        if (recParts.length > 1 && actParts.length > 1) {
            Set<String> recBlue = parseBallSet(recParts[1]);
            Set<String> actBlue = parseBallSet(actParts[1]);
            hitExtra = (int) recBlue.stream().filter(actBlue::contains).count();
        }
        return new int[]{hitMain, hitExtra};
    }

    private int[] calculateHitsDlt(String rec, String actual) {
        String[] recParts = rec.replace(" ", "").split("\\+");
        String[] actParts = actual.replace(" ", "").split("\\+");

        Set<String> recFront = parseBallSet(recParts[0]);
        Set<String> actFront = parseBallSet(actParts[0]);
        int hitMain = (int) recFront.stream().filter(actFront::contains).count();

        int hitExtra = 0;
        if (recParts.length > 1 && actParts.length > 1) {
            Set<String> recBack = parseBallSet(recParts[1]);
            Set<String> actBack = parseBallSet(actParts[1]);
            hitExtra = (int) recBack.stream().filter(actBack::contains).count();
        }
        return new int[]{hitMain, hitExtra};
    }

    private int[] calculateHitsPositional(String rec, String actual) {
        // rec: "1 2 3"  actual: "123" or "1,2,3"
        String recClean = rec.replace(" ", "").replace(",", "");
        String actClean = actual.replace(" ", "").replace(",", "");
        int hits = 0;
        for (int i = 0; i < Math.min(recClean.length(), actClean.length()); i++) {
            if (recClean.charAt(i) == actClean.charAt(i)) hits++;
        }
        return new int[]{hits, 0};
    }

    private int[] calculateHitsQlc(String rec, String actual) {
        Set<String> recNums = parseBallSet(rec.replace(" ", ""));
        Set<String> actNums = parseBallSet(actual.replace(" ", ""));
        int hits = (int) recNums.stream().filter(actNums::contains).count();
        return new int[]{hits, 0};
    }

    private Set<String> parseBallSet(String s) {
        return Arrays.stream(s.replace(",", " ").replace("+", " ").trim().split("\\s+"))
                .filter(v -> !v.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    private Map<String, Object> toMap(RecommendationHistory rec) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("strategy", rec.getStrategyName());
        m.put("numbers", rec.getRecommendedNumbers());
        m.put("actual", rec.getActualNumbers());
        m.put("hitMain", rec.getHitMain());
        m.put("hitExtra", rec.getHitExtra());
        m.put("date", rec.getRecommendDate());
        return m;
    }
}
