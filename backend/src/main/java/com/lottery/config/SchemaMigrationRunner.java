package com.lottery.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMigrationRunner {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrate() {
        ensureAuditColumns("lottery_result");
        ensureAuditColumns("lottery_demo_result");
        ensureFetchHistoryTaskTable();
        ensureFetchHistoryDetailTable();
        ensureFetchHistoryIndexes();
    }

    private void ensureAuditColumns(String tableName) {
        List<String> existingColumns = jdbcTemplate.query(
                "PRAGMA table_info(" + tableName + ")",
                (rs, rowNum) -> rs.getString("name")
        );

        addColumnIfMissing(tableName, existingColumns, "created_at", "TEXT");
        addColumnIfMissing(tableName, existingColumns, "created_by", "TEXT");
        addColumnIfMissing(tableName, existingColumns, "updated_at", "TEXT");
        addColumnIfMissing(tableName, existingColumns, "updated_by", "TEXT");
    }

    private void ensureFetchHistoryTaskTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS fetch_history_task (" +
                "task_id TEXT PRIMARY KEY, " +
                "trigger_source TEXT NOT NULL, " +
                "type TEXT NOT NULL, " +
                "scope TEXT NOT NULL, " +
                "mode TEXT NOT NULL, " +
                "status TEXT NOT NULL, " +
                "current_type TEXT, " +
                "current_page INTEGER DEFAULT 0, " +
                "total_fetched INTEGER DEFAULT 0, " +
                "inserted INTEGER DEFAULT 0, " +
                "updated INTEGER DEFAULT 0, " +
                "completed_types INTEGER DEFAULT 0, " +
                "total_types INTEGER DEFAULT 0, " +
                "error TEXT, " +
                "started_at TEXT, " +
                "finished_at TEXT, " +
                "created_at TEXT, " +
                "updated_at TEXT" +
                ")");
        List<String> existingColumns = jdbcTemplate.query(
                "PRAGMA table_info(fetch_history_task)",
                (rs, rowNum) -> rs.getString("name")
        );
        addColumnIfMissing("fetch_history_task", existingColumns, "trigger_source", "TEXT");
        addColumnIfMissing("fetch_history_task", existingColumns, "type", "TEXT");
        addColumnIfMissing("fetch_history_task", existingColumns, "scope", "TEXT");
        addColumnIfMissing("fetch_history_task", existingColumns, "mode", "TEXT");
        addColumnIfMissing("fetch_history_task", existingColumns, "status", "TEXT");
        addColumnIfMissing("fetch_history_task", existingColumns, "current_type", "TEXT");
        addColumnIfMissing("fetch_history_task", existingColumns, "current_page", "INTEGER DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existingColumns, "total_fetched", "INTEGER DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existingColumns, "inserted", "INTEGER DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existingColumns, "updated", "INTEGER DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existingColumns, "completed_types", "INTEGER DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existingColumns, "total_types", "INTEGER DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existingColumns, "error", "TEXT");
        addColumnIfMissing("fetch_history_task", existingColumns, "started_at", "TEXT");
        addColumnIfMissing("fetch_history_task", existingColumns, "finished_at", "TEXT");
        addColumnIfMissing("fetch_history_task", existingColumns, "created_at", "TEXT");
        addColumnIfMissing("fetch_history_task", existingColumns, "updated_at", "TEXT");
    }

    private void ensureFetchHistoryDetailTable() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS fetch_history_detail (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "task_id TEXT NOT NULL, " +
                "lottery_type TEXT NOT NULL, " +
                "name TEXT, " +
                "scope TEXT, " +
                "status TEXT NOT NULL, " +
                "current_page INTEGER DEFAULT 0, " +
                "total_fetched INTEGER DEFAULT 0, " +
                "inserted INTEGER DEFAULT 0, " +
                "updated INTEGER DEFAULT 0, " +
                "error TEXT, " +
                "sort_order INTEGER DEFAULT 0, " +
                "created_at TEXT, " +
                "updated_at TEXT, " +
                "UNIQUE(task_id, lottery_type)" +
                ")");
        List<String> existingColumns = jdbcTemplate.query(
                "PRAGMA table_info(fetch_history_detail)",
                (rs, rowNum) -> rs.getString("name")
        );
        addColumnIfMissing("fetch_history_detail", existingColumns, "task_id", "TEXT");
        addColumnIfMissing("fetch_history_detail", existingColumns, "lottery_type", "TEXT");
        addColumnIfMissing("fetch_history_detail", existingColumns, "name", "TEXT");
        addColumnIfMissing("fetch_history_detail", existingColumns, "scope", "TEXT");
        addColumnIfMissing("fetch_history_detail", existingColumns, "status", "TEXT");
        addColumnIfMissing("fetch_history_detail", existingColumns, "current_page", "INTEGER DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existingColumns, "total_fetched", "INTEGER DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existingColumns, "inserted", "INTEGER DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existingColumns, "updated", "INTEGER DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existingColumns, "error", "TEXT");
        addColumnIfMissing("fetch_history_detail", existingColumns, "sort_order", "INTEGER DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existingColumns, "created_at", "TEXT");
        addColumnIfMissing("fetch_history_detail", existingColumns, "updated_at", "TEXT");
    }

    private void ensureFetchHistoryIndexes() {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_fetch_history_task_started_at ON fetch_history_task(started_at DESC)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_fetch_history_task_status ON fetch_history_task(status)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_fetch_history_task_trigger_source ON fetch_history_task(trigger_source)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_fetch_history_task_type ON fetch_history_task(type)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_fetch_history_detail_task_id ON fetch_history_detail(task_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_fetch_history_detail_type ON fetch_history_detail(lottery_type)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_fetch_history_detail_sort_order ON fetch_history_detail(task_id, sort_order)");
    }

    private void addColumnIfMissing(String tableName, List<String> existingColumns, String columnName, String definition) {
        if (existingColumns.contains(columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        log.info("已为表 [{}] 增加字段 [{}]", tableName, columnName);
    }
}
