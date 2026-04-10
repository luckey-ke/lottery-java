package com.lottery.task;

import com.lottery.service.FetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每日开奖数据定时拉取任务
 * <p>
 * 使用 Spring @Scheduled，由 Spring 管理线程池生命周期。
 * 拉取操作本身异步执行（FetchService 内部线程池），本方法不阻塞调度线程。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyFetchTask {

    private final FetchService fetchService;

    @Scheduled(cron = "${lottery.fetch.cron:0 30 22 * * ?}")
    public void dailyFetch() {
        log.info("[定时任务] 开始拉取开奖数据...");
        try {
            fetchService.fetchLatest();
            log.info("[定时任务] 已提交拉取任务");
        } catch (Exception e) {
            log.error("[定时任务] 提交拉取任务异常: {}", e.getMessage(), e);
        }
    }
}
