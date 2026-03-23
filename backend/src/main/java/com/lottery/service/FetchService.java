package com.lottery.service;

import com.lottery.entity.LotteryResult;

import java.util.List;
import java.util.Map;

public interface FetchService {
    Map<String, Object> fetchAll(String scope, Integer count);
    Map<String, Object> fetch(String lotteryType, String scope, Integer count);
    Map<String, Object> startFetchAllTask(String scope, Integer count);
    Map<String, Object> startFetchTask(String lotteryType, String scope, Integer count);
    Map<String, Object> getFetchTask(String taskId);
    Map<String, Object> listFetchHistory(String status, String triggerSource, String type, int limit, int offset);
    Map<String, Object> getFetchHistory(String taskId);
    Map<String, Object> fetchAllDemo(int count);
    Map<String, Object> fetchDemo(String lotteryType, int count);
    void fetchLatest();
    List<LotteryResult> generateDemo(String type, int count);
}
