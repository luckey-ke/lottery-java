package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("fetch_history_detail")
public class FetchHistoryDetail {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String taskId;
    private String lotteryType;
    private String name;
    private String scope;
    private String status;
    private Integer currentPage;
    private Integer totalFetched;
    private Integer inserted;
    private Integer updated;
    private String error;
    private Integer sortOrder;
    private String createdAt;
    private String updatedAt;
}
