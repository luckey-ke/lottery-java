package com.lottery.mapper;

import com.lottery.entity.LotteryResult;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LotteryResultMapper {

    @Insert("INSERT INTO lottery_result(lottery_type, draw_num, draw_date, numbers, extra_info, fetched_at, created_at, created_by, updated_at, updated_by) " +
            "VALUES(#{lotteryType}, #{drawNum}, #{drawDate}, #{numbers}, #{extraInfo}, #{fetchedAt}, #{createdAt}, #{createdBy}, #{updatedAt}, #{updatedBy}) " +
            "ON CONFLICT(lottery_type, draw_num) DO UPDATE SET " +
            "draw_date = excluded.draw_date, " +
            "numbers = excluded.numbers, " +
            "extra_info = excluded.extra_info, " +
            "fetched_at = excluded.fetched_at, " +
            "updated_at = excluded.updated_at, " +
            "updated_by = excluded.updated_by")
    int upsertReal(LotteryResult result);

    @Insert("INSERT INTO lottery_demo_result(lottery_type, draw_num, draw_date, numbers, extra_info, fetched_at, created_at, created_by, updated_at, updated_by) " +
            "VALUES(#{lotteryType}, #{drawNum}, #{drawDate}, #{numbers}, #{extraInfo}, #{fetchedAt}, #{createdAt}, #{createdBy}, #{updatedAt}, #{updatedBy}) " +
            "ON CONFLICT(lottery_type, draw_num) DO UPDATE SET " +
            "draw_date = excluded.draw_date, " +
            "numbers = excluded.numbers, " +
            "extra_info = excluded.extra_info, " +
            "fetched_at = excluded.fetched_at, " +
            "updated_at = excluded.updated_at, " +
            "updated_by = excluded.updated_by")
    int upsertDemo(LotteryResult result);

    @Select("SELECT COUNT(*) FROM lottery_result WHERE lottery_type = #{type} AND draw_num = #{drawNum}")
    int existsReal(@Param("type") String type, @Param("drawNum") String drawNum);

    @Select("SELECT COUNT(*) FROM lottery_demo_result WHERE lottery_type = #{type} AND draw_num = #{drawNum}")
    int existsDemo(@Param("type") String type, @Param("drawNum") String drawNum);

    @Select("SELECT * FROM lottery_result WHERE lottery_type = #{type} ORDER BY draw_num DESC LIMIT #{limit} OFFSET #{offset}")
    List<LotteryResult> findByType(@Param("type") String type, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM lottery_result ORDER BY draw_date DESC, draw_num DESC LIMIT #{limit} OFFSET #{offset}")
    List<LotteryResult> findAll(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM lottery_demo_result WHERE lottery_type = #{type} ORDER BY draw_num DESC LIMIT #{limit} OFFSET #{offset}")
    List<LotteryResult> findDemoByType(@Param("type") String type, @Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT * FROM lottery_demo_result ORDER BY draw_date DESC, draw_num DESC LIMIT #{limit} OFFSET #{offset}")
    List<LotteryResult> findAllDemo(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM lottery_result")
    int countAll();

    @Select("SELECT COUNT(*) FROM lottery_result WHERE lottery_type = #{type}")
    int countByType(@Param("type") String type);

    @Select("SELECT COUNT(*) FROM lottery_demo_result")
    int countAllDemo();

    @Select("SELECT COUNT(*) FROM lottery_demo_result WHERE lottery_type = #{type}")
    int countDemoByType(@Param("type") String type);

    @Select("SELECT draw_num FROM lottery_result WHERE lottery_type = #{type} ORDER BY draw_num DESC LIMIT 1")
    String findLatestDrawNum(@Param("type") String type);

    @Select("SELECT draw_num FROM lottery_demo_result WHERE lottery_type = #{type} ORDER BY draw_num DESC LIMIT 1")
    String findLatestDemoDrawNum(@Param("type") String type);

    @Select("SELECT draw_num, draw_date, numbers FROM lottery_result WHERE lottery_type = #{type} ORDER BY draw_num ASC")
    List<LotteryResult> findAllNumbers(@Param("type") String type);

    @Select("SELECT draw_num, draw_date, numbers FROM lottery_demo_result WHERE lottery_type = #{type} ORDER BY draw_num ASC")
    List<LotteryResult> findAllDemoNumbers(@Param("type") String type);
}
