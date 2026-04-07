package com.lottery.entity;

import lombok.Data;

@Data
public class RecommendationHistory {
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
