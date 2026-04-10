package com.lottery.service.impl.fetcher;

/**
 * 数据抓取进度回调
 */
@FunctionalInterface
public interface FetchProgressCallback {
    void onProgress(int currentPage, int totalFetched, int inserted, int updated);
}
