package com.lottery.entity;

import lombok.Data;

@Data
public class FetchHistoryTask {
    private String taskId;
    private String triggerSource;
    private String type;
    private String scope;
    private String mode;
    private String status;
    private String currentType;
    private Integer currentPage;
    private Integer totalFetched;
    private Integer inserted;
    private Integer updated;
    private Integer completedTypes;
    private Integer totalTypes;
    private String error;
    private String startedAt;
    private String finishedAt;
    private String createdAt;
    private String updatedAt;
}
