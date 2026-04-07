package com.lottery.service;

import java.util.Map;

public interface FetchService {
    Map<String, Object> fetchAll(String scope, Integer count);
    Map<String, Object> fetch(String lotteryType, String scope, Integer count);
    Map<String, Object> startFetchAllTask(String scope, Integer count);
    Map<String, Object> startFetchTask(String lotteryType, String scope, Integer count);
    Map<String, Object> getFetchTask(String taskId);
    Map<String, Object> listFetchHistory(String status, String triggerSource, String type, int limit, int offset);
    Map<String, Object> getFetchHistory(String taskId);
    void fetchLatest();
}
