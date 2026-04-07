package com.lottery.mapper;

import com.lottery.entity.RecommendationHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecommendationHistoryMapper {

    int upsert(RecommendationHistory record);

    @Select("SELECT * FROM recommendation_history WHERE lottery_type = #{type} AND recommend_date = #{date} ORDER BY strategy_index")
    List<RecommendationHistory> findByDate(@Param("type") String type, @Param("date") String date);

    @Select("SELECT * FROM recommendation_history WHERE lottery_type = #{type} ORDER BY recommend_date DESC, strategy_index LIMIT #{limit} OFFSET #{offset}")
    List<RecommendationHistory> listByType(@Param("type") String type, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(DISTINCT recommend_date) FROM recommendation_history WHERE lottery_type = #{type}")
    int countDates(@Param("type") String type);

    @Select("SELECT DISTINCT recommend_date FROM recommendation_history WHERE lottery_type = #{type} ORDER BY recommend_date DESC LIMIT #{limit} OFFSET #{offset}")
    List<String> listDates(@Param("type") String type, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM recommendation_history WHERE lottery_type = #{type} AND recommend_date = #{date}")
    List<RecommendationHistory> findByTypeAndDate(@Param("type") String type, @Param("date") String date);

    // 统计：按策略分组的命中率
    @Select("SELECT strategy_name, " +
            "COUNT(*) as total, " +
            "SUM(CASE WHEN hit_main > 0 OR hit_extra > 0 THEN 1 ELSE 0 END) as hit_count, " +
            "AVG(hit_main) as avg_hit_main, " +
            "MAX(hit_main) as max_hit_main, " +
            "AVG(hit_extra) as avg_hit_extra, " +
            "MAX(hit_extra) as max_hit_extra " +
            "FROM recommendation_history " +
            "WHERE lottery_type = #{type} AND actual_numbers IS NOT NULL " +
            "GROUP BY strategy_name")
    List<java.util.Map<String, Object>> getHitStats(@Param("type") String type);

    // 获取未匹配的记录
    @Select("SELECT * FROM recommendation_history WHERE lottery_type = #{type} AND actual_numbers IS NULL ORDER BY recommend_date")
    List<RecommendationHistory> findUnmatched(@Param("type") String type);
}
