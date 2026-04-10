package com.lottery.service;

import com.lottery.entity.LotteryResult;
import com.lottery.mapper.LotteryResultMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryResultService {

    @Value("${lottery.audit.created-by:system}")
    private String createdBy;

    @Value("${lottery.audit.updated-by:system}")
    private String updatedBy;
    private static final DateTimeFormatter AUDIT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LotteryResultMapper mapper;
    private final Map<String, List<LotteryResult>> allNumbersCache = new ConcurrentHashMap<>();

    public SaveOutcome saveReal(LotteryResult result) {
        boolean exists = mapper.existsReal(result.getLotteryType(), result.getDrawNum()) > 0;

        String now = LocalDateTime.now().format(AUDIT_TIME_FORMATTER);
        result.setFetchedAt(now);
        result.setUpdatedAt(now);
        result.setUpdatedBy(updatedBy);
        if (!exists) {
            result.setCreatedAt(now);
            result.setCreatedBy(createdBy);
        }

        mapper.upsertReal(result);
        evictCache(result.getLotteryType());
        return exists ? SaveOutcome.UPDATED : SaveOutcome.INSERTED;
    }

    public List<LotteryResult> queryReal(String type, int limit, int offset) {
        if (type == null || type.isBlank()) {
            return mapper.findAll(limit, offset);
        }
        return mapper.findByType(type, limit, offset);
    }

    public int countReal(String type) {
        if (type == null || type.isBlank()) {
            return mapper.countAll();
        }
        return mapper.countByType(type);
    }

    public String latestRealDrawNum(String type) {
        return mapper.findLatestDrawNum(type);
    }

    public LotteryResult latestRealResult(String type) {
        List<LotteryResult> list = queryReal(type, 1, 0);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<LotteryResult> allRealNumbers(String type) {
        return allNumbersCache.computeIfAbsent(type, k -> mapper.findAllNumbers(type));
    }

    /** 缓存失效 - 在 saveReal 调用后清除 */
    public void evictCache(String type) {
        allNumbersCache.remove(type);
    }

    public enum SaveOutcome {
        INSERTED,
        UPDATED
    }
}
