package com.lottery.entity;

import lombok.Data;

@Data
public class LotteryResult {
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
