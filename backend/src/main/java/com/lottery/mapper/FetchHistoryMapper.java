package com.lottery.mapper;

import com.lottery.entity.FetchHistoryDetail;
import com.lottery.entity.FetchHistoryTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface FetchHistoryMapper {

    int upsertTask(FetchHistoryTask task);

    int upsertDetail(FetchHistoryDetail detail);

    @Select({
            "<script>",
            "SELECT task_id AS taskId, trigger_source AS triggerSource, type, scope, mode, status, current_type AS currentType, current_page AS currentPage, total_fetched AS totalFetched, inserted, updated, completed_types AS completedTypes, total_types AS totalTypes, error, started_at AS startedAt, finished_at AS finishedAt, created_at AS createdAt, updated_at AS updatedAt",
            "FROM fetch_history_task",
            "WHERE 1 = 1",
            "<if test='status != null and status != \"\"'> AND status = #{status}</if>",
            "<if test='triggerSource != null and triggerSource != \"\"'> AND trigger_source = #{triggerSource}</if>",
            "<if test='type != null and type != \"\"'> AND (type = #{type} OR EXISTS (SELECT 1 FROM fetch_history_detail d WHERE d.task_id = fetch_history_task.task_id AND d.lottery_type = #{type}))</if>",
            "ORDER BY datetime(COALESCE(started_at, created_at)) DESC, task_id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<FetchHistoryTask> listTasks(@Param("status") String status,
                                     @Param("triggerSource") String triggerSource,
                                     @Param("type") String type,
                                     @Param("limit") int limit,
                                     @Param("offset") int offset);

    @Select({
            "<script>",
            "SELECT COUNT(*)",
            "FROM fetch_history_task",
            "WHERE 1 = 1",
            "<if test='status != null and status != \"\"'> AND status = #{status}</if>",
            "<if test='triggerSource != null and triggerSource != \"\"'> AND trigger_source = #{triggerSource}</if>",
            "<if test='type != null and type != \"\"'> AND (type = #{type} OR EXISTS (SELECT 1 FROM fetch_history_detail d WHERE d.task_id = fetch_history_task.task_id AND d.lottery_type = #{type}))</if>",
            "</script>"
    })
    int countTasks(@Param("status") String status,
                   @Param("triggerSource") String triggerSource,
                   @Param("type") String type);

    @Select("SELECT task_id AS taskId, trigger_source AS triggerSource, type, scope, mode, status, current_type AS currentType, current_page AS currentPage, total_fetched AS totalFetched, inserted, updated, completed_types AS completedTypes, total_types AS totalTypes, error, started_at AS startedAt, finished_at AS finishedAt, created_at AS createdAt, updated_at AS updatedAt FROM fetch_history_task WHERE task_id = #{taskId}")
    FetchHistoryTask findTaskById(@Param("taskId") String taskId);

    @Select("SELECT task_id, status FROM fetch_history_task WHERE status IN ('pending', 'running')")
    List<java.util.Map<String, Object>> findUnfinishedTaskIds();

    @Select("SELECT id, task_id AS taskId, lottery_type AS lotteryType, name, scope, status, current_page AS currentPage, total_fetched AS totalFetched, inserted, updated, error, sort_order AS sortOrder, created_at AS createdAt, updated_at AS updatedAt FROM fetch_history_detail WHERE task_id = #{taskId} ORDER BY sort_order ASC, id ASC")
    List<FetchHistoryDetail> listDetailsByTaskId(@Param("taskId") String taskId);
}
