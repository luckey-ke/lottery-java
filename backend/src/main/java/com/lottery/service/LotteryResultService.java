package com.lottery.service;

import com.lottery.entity.LotteryResult;
import com.lottery.mapper.LotteryResultMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LotteryResultService {

    private static final String CREATED_BY = "666";
    private static final String UPDATED_BY = "999";
    private static final DateTimeFormatter AUDIT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LotteryResultMapper mapper;
    private final Object saveLock = new Object();

    public SaveOutcome saveReal(LotteryResult result) {
        synchronized (saveLock) {
            boolean exists = mapper.existsReal(result.getLotteryType(), result.getDrawNum()) > 0;

            String now = LocalDateTime.now().format(AUDIT_TIME_FORMATTER);
            result.setFetchedAt(now);
            result.setUpdatedAt(now);
            result.setUpdatedBy(UPDATED_BY);
            if (!exists) {
                result.setCreatedAt(now);
                result.setCreatedBy(CREATED_BY);
            }

            mapper.upsertReal(result);
            return exists ? SaveOutcome.UPDATED : SaveOutcome.INSERTED;
        }
    }

    public List<LotteryResult> query(String type, int limit, int offset) {
        return queryReal(type, limit, offset);
    }

    public List<LotteryResult> queryReal(String type, int limit, int offset) {
        if (type == null || type.isBlank()) {
            return mapper.findAll(limit, offset);
        }
        return mapper.findByType(type, limit, offset);
    }

    public int count(String type) {
        return countReal(type);
    }

    public int countReal(String type) {
        if (type == null || type.isBlank()) {
            return mapper.countAll();
        }
        return mapper.countByType(type);
    }

    public String latestDrawNum(String type) {
        return latestRealDrawNum(type);
    }

    public String latestRealDrawNum(String type) {
        return mapper.findLatestDrawNum(type);
    }

    public LotteryResult latestRealResult(String type) {
        List<LotteryResult> list = queryReal(type, 1, 0);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<LotteryResult> allNumbers(String type) {
        return allRealNumbers(type);
    }

    public List<LotteryResult> allRealNumbers(String type) {
        return mapper.findAllNumbers(type);
    }

    public enum SaveOutcome {
        INSERTED,
        UPDATED
    }
}
