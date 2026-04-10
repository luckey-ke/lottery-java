package com.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lottery.entity.RecommendationHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface RecommendationHistoryMapper extends BaseMapper<RecommendationHistory> {

    /** upsert (XML 实现，兼容 SQLite/MySQL) */
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
    List<Map<String, Object>> getHitStats(@Param("type") String type);

    @Select("SELECT * FROM recommendation_history WHERE lottery_type = #{type} AND actual_numbers IS NULL ORDER BY recommend_date")
    List<RecommendationHistory> findUnmatched(@Param("type") String type);
}
