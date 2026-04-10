package com.lottery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lottery.entity.LotteryResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface LotteryResultMapper extends BaseMapper<LotteryResult> {

    /** 自定义 UPSERT（XML 实现，兼容 SQLite/MySQL） */
    int upsertReal(LotteryResult result);

    /** 检查记录是否存在 */
    int existsReal(@Param("type") String type, @Param("drawNum") String drawNum);

    /** 按彩种分页查询 */
    List<LotteryResult> findByType(@Param("type") String type, @Param("limit") int limit, @Param("offset") int offset);

    /** 全部分页查询 */
    List<LotteryResult> findAll(@Param("limit") int limit, @Param("offset") int offset);

    /** 总数 */
    int countAll();

    /** 按彩种计数 */
    int countByType(@Param("type") String type);

    /** 最新期号 */
    String findLatestDrawNum(@Param("type") String type);

    /** 查询某彩种最新一条完整记录 */
    LotteryResult findLatestByType(@Param("type") String type);

    /** 按日期范围查询 */
    List<LotteryResult> findByTypeAndDateRange(@Param("type") String type, @Param("startDate") String startDate, @Param("endDate") String endDate);

    /** 按期号查询单条 */
    LotteryResult findByTypeAndDrawNum(@Param("type") String type, @Param("drawNum") String drawNum);

    /** 全量号码查询（用于分析，按期号升序） */
    List<LotteryResult> findAllNumbers(@Param("type") String type);

    /** 按 ID 查询 */
    LotteryResult findById(@Param("id") Integer id);

    /** 删除某条记录 */
    int deleteById(@Param("id") Integer id);

    /** 删除某彩种全部记录 */
    int deleteByType(@Param("type") String type);

    /** 批量插入（XML 实现） */
    int batchInsert(List<LotteryResult> list);

    /** 各彩种数据量统计 */
    List<Map<String, Object>> countByAllTypes();
}
