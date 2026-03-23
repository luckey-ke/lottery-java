package com.lottery.task;

import com.lottery.service.FetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyFetchTask {

    private final FetchService fetchService;

    // 每天 22:30 执行（可通过 lottery.fetch.hour/minute 配置）
    @Scheduled(cron = "0 30 22 * * ?")
    public void dailyFetch() {
        log.info("[定时任务] 开始拉取开奖数据...");
        try {
            fetchService.fetchLatest();
            log.info("[定时任务] 完成");
        } catch (Exception e) {
            log.error("[定时任务] 异常: {}", e.getMessage(), e);
        }
    }
}
