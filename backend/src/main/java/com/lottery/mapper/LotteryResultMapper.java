package com.lottery.mapper;

import com.lottery.entity.LotteryResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface LotteryResultMapper {

    int upsertReal(LotteryResult result);

    int existsReal(@Param("type") String type, @Param("drawNum") String drawNum);

    List<LotteryResult> findByType(@Param("type") String type, @Param("limit") int limit, @Param("offset") int offset);

    List<LotteryResult> findAll(@Param("limit") int limit, @Param("offset") int offset);

    int countAll();

    int countByType(@Param("type") String type);

    String findLatestDrawNum(@Param("type") String type);

    LotteryResult findLatestByType(@Param("type") String type);

    List<LotteryResult> findByTypeAndDateRange(@Param("type") String type,
                                                @Param("startDate") String startDate,
                                                @Param("endDate") String endDate);

    LotteryResult findByTypeAndDrawNum(@Param("type") String type, @Param("drawNum") String drawNum);

    List<LotteryResult> findAllNumbers(@Param("type") String type);

    LotteryResult findById(@Param("id") Integer id);

    int deleteById(@Param("id") Integer id);

    int deleteByType(@Param("type") String type);

    int batchInsert(@Param("list") List<LotteryResult> list);

    List<Map<String, Object>> countByAllTypes();
}
