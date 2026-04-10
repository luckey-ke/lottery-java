package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("lottery_result")
public class LotteryResult {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String lotteryType;
    private String drawNum;
    private String drawDate;
    private String numbers;
    private String extraInfo;
    private String fetchedAt;
    private String createdAt;
    private String createdBy;
    private String updatedAt;
    private String updatedBy;
}
