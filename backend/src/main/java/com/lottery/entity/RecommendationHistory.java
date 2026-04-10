package com.lottery.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("recommendation_history")
public class RecommendationHistory {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String lotteryType;
    private String recommendDate;
    private String strategyName;
    private Integer strategyIndex;
    private String recommendedNumbers;
    private String actualNumbers;
    private Integer hitMain;
    private Integer hitExtra;
    private String createdAt;
    private String updatedAt;
}
