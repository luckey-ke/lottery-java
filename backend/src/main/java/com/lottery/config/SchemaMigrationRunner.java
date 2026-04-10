package com.lottery.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMigrationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    private boolean sqlite;

    @PostConstruct
    public void migrate() {
        this.sqlite = detectSqlite();
        log.info("[SchemaMigration] 检测到数据库类型: {}", sqlite ? "SQLite" : "MySQL/Other");

        ensureAuditColumns("lottery_result");
        ensureFetchHistoryTaskTable();
        ensureFetchHistoryDetailTable();
        ensureFetchHistoryIndexes();
        ensureRecommendationHistoryTable();
    }

    // ===== 数据库类型检测 =====

    private boolean detectSqlite() {
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            return product != null && product.equalsIgnoreCase("SQLite");
        } catch (SQLException e) {
            log.warn("无法检测数据库类型，默认按 SQLite 处理: {}", e.getMessage());
            return true;
        }
    }

    // ===== 获取表列名 =====

    private List<String> getExistingColumns(String tableName) {
        if (sqlite) {
            return jdbcTemplate.query(
                    "PRAGMA table_info(" + tableName + ")",
                    (rs, rowNum) -> rs.getString("name")
            );
        } else {
            // MySQL / 其他标准数据库
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                // 获取当前数据库名（catalog）
                String catalog = conn.getCatalog();
                List<String> columns = new ArrayList<>();
                try (ResultSet rs = meta.getColumns(catalog, null, tableName, null)) {
                    while (rs.next()) {
                        columns.add(rs.getString("COLUMN_NAME"));
                    }
                }
                return columns;
            } catch (SQLException e) {
                log.warn("获取表 [{}] 列信息失败: {}", tableName, e.getMessage());
                return List.of();
            }
        }
    }

    // ===== 列类型定义 =====

    private String autoIncrementColumn() {
        return sqlite ? "INTEGER PRIMARY KEY AUTOINCREMENT" : "INT AUTO_INCREMENT PRIMARY KEY";
    }

    private String textType() {
        return sqlite ? "TEXT" : "VARCHAR(255)";
    }

    // ===== 表创建 =====

    private void ensureAuditColumns(String tableName) {
        List<String> existing = getExistingColumns(tableName);
        addColumnIfMissing(tableName, existing, "created_at", textType());
        addColumnIfMissing(tableName, existing, "created_by", textType());
        addColumnIfMissing(tableName, existing, "updated_at", textType());
        addColumnIfMissing(tableName, existing, "updated_by", textType());
    }

    private void ensureFetchHistoryTaskTable() {
        String pk = sqlite ? "task_id TEXT PRIMARY KEY" : "task_id VARCHAR(128) PRIMARY KEY";
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS fetch_history_task (" +
                pk + ", " +
                "trigger_source " + textType() + " NOT NULL, " +
                "type " + textType() + " NOT NULL, " +
                "scope " + textType() + " NOT NULL, " +
                "mode " + textType() + " NOT NULL, " +
                "status " + textType() + " NOT NULL, " +
                "current_type " + textType() + ", " +
                "current_page " + (sqlite ? "INTEGER" : "INT") + " DEFAULT 0, " +
                "total_fetched " + (sqlite ? "INTEGER" : "INT") + " DEFAULT 0, " +
                "inserted " + (sqlite ? "INTEGER" : "INT") + " DEFAULT 0, " +
                "updated " + (sqlite ? "INTEGER" : "INT") + " DEFAULT 0, " +
                "completed_types " + (sqlite ? "INTEGER" : "INT") + " DEFAULT 0, " +
                "total_types " + (sqlite ? "INTEGER" : "INT") + " DEFAULT 0, " +
                "error TEXT, " +
                "started_at " + textType() + ", " +
                "finished_at " + textType() + ", " +
                "created_at " + textType() + ", " +
                "updated_at " + textType() +
                ")");

        List<String> existing = getExistingColumns("fetch_history_task");
        addColumnIfMissing("fetch_history_task", existing, "trigger_source", textType());
        addColumnIfMissing("fetch_history_task", existing, "type", textType());
        addColumnIfMissing("fetch_history_task", existing, "scope", textType());
        addColumnIfMissing("fetch_history_task", existing, "mode", textType());
        addColumnIfMissing("fetch_history_task", existing, "status", textType());
        addColumnIfMissing("fetch_history_task", existing, "current_type", textType());
        addColumnIfMissing("fetch_history_task", existing, "current_page", (sqlite ? "INTEGER" : "INT") + " DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existing, "total_fetched", (sqlite ? "INTEGER" : "INT") + " DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existing, "inserted", (sqlite ? "INTEGER" : "INT") + " DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existing, "updated", (sqlite ? "INTEGER" : "INT") + " DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existing, "completed_types", (sqlite ? "INTEGER" : "INT") + " DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existing, "total_types", (sqlite ? "INTEGER" : "INT") + " DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existing, "error", "TEXT");
        addColumnIfMissing("fetch_history_task", existing, "started_at", textType());
        addColumnIfMissing("fetch_history_task", existing, "finished_at", textType());
        addColumnIfMissing("fetch_history_task", existing, "created_at", textType());
        addColumnIfMissing("fetch_history_task", existing, "updated_at", textType());
    }

    private void ensureFetchHistoryDetailTable() {
        String idCol = sqlite ? "id " + autoIncrementColumn() : "id " + autoIncrementColumn();
        String uniqueConstraint = sqlite
                ? "UNIQUE(task_id, lottery_type)"
                : "UNIQUE KEY uk_task_type (task_id, lottery_type)";
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS fetch_history_detail (" +
                idCol + ", " +
                "task_id " + (sqlite ? "TEXT" : "VARCHAR(128)") + " NOT NULL, " +
                "lottery_type " + textType() + " NOT NULL, " +
                "name " + textType() + ", " +
                "scope " + textType() + ", " +
                "status " + textType() + " NOT NULL, " +
                "current_page " + (sqlite ? "INTEGER" : "INT") + " DEFAULT 0, " +
                "total_fetched " + (sqlite ? "INTEGER" : "INT") + " DEFAULT 0, " +
                "inserted " + (sqlite ? "INTEGER" : "INT") + " DEFAULT 0, " +
                "updated " + (sqlite ? "INTEGER" : "INT") + " DEFAULT 0, " +
                "error TEXT, " +
                "sort_order " + (sqlite ? "INTEGER" : "INT") + " DEFAULT 0, " +
                "created_at " + textType() + ", " +
                "updated_at " + textType() + ", " +
                uniqueConstraint +
                ")");

        List<String> existing = getExistingColumns("fetch_history_detail");
        addColumnIfMissing("fetch_history_detail", existing, "task_id", (sqlite ? "TEXT" : "VARCHAR(128)"));
        addColumnIfMissing("fetch_history_detail", existing, "lottery_type", textType());
        addColumnIfMissing("fetch_history_detail", existing, "name", textType());
        addColumnIfMissing("fetch_history_detail", existing, "scope", textType());
        addColumnIfMissing("fetch_history_detail", existing, "status", textType());
        addColumnIfMissing("fetch_history_detail", existing, "current_page", (sqlite ? "INTEGER" : "INT") + " DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existing, "total_fetched", (sqlite ? "INTEGER" : "INT") + " DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existing, "inserted", (sqlite ? "INTEGER" : "INT") + " DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existing, "updated", (sqlite ? "INTEGER" : "INT") + " DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existing, "error", "TEXT");
        addColumnIfMissing("fetch_history_detail", existing, "sort_order", (sqlite ? "INTEGER" : "INT") + " DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existing, "created_at", textType());
        addColumnIfMissing("fetch_history_detail", existing, "updated_at", textType());
    }

    private void ensureFetchHistoryIndexes() {
        String ifNotExists = sqlite ? "IF NOT EXISTS" : "IF NOT EXISTS";
        executeIndex("CREATE INDEX " + ifNotExists + " idx_fetch_history_task_started_at ON fetch_history_task(started_at DESC)");
        executeIndex("CREATE INDEX " + ifNotExists + " idx_fetch_history_task_status ON fetch_history_task(status)");
        executeIndex("CREATE INDEX " + ifNotExists + " idx_fetch_history_task_trigger_source ON fetch_history_task(trigger_source)");
        executeIndex("CREATE INDEX " + ifNotExists + " idx_fetch_history_task_type ON fetch_history_task(type)");
        executeIndex("CREATE INDEX " + ifNotExists + " idx_fetch_history_detail_task_id ON fetch_history_detail(task_id)");
        executeIndex("CREATE INDEX " + ifNotExists + " idx_fetch_history_detail_type ON fetch_history_detail(lottery_type)");
        executeIndex("CREATE INDEX " + ifNotExists + " idx_fetch_history_detail_sort_order ON fetch_history_detail(task_id, sort_order)");
    }

    private void ensureRecommendationHistoryTable() {
        String uniqueConstraint = sqlite
                ? "UNIQUE(lottery_type, recommend_date, strategy_index)"
                : "UNIQUE KEY uk_rec_type_date_idx (lottery_type, recommend_date, strategy_index)";
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS recommendation_history (" +
                "id " + autoIncrementColumn() + ", " +
                "lottery_type " + textType() + " NOT NULL, " +
                "recommend_date " + textType() + " NOT NULL, " +
                "strategy_name " + textType() + " NOT NULL, " +
                "strategy_index " + (sqlite ? "INTEGER" : "INT") + " NOT NULL, " +
                "recommended_numbers TEXT NOT NULL, " +
                "actual_numbers TEXT, " +
                "hit_main " + (sqlite ? "INTEGER" : "INT") + " DEFAULT 0, " +
                "hit_extra " + (sqlite ? "INTEGER" : "INT") + " DEFAULT 0, " +
                "created_at " + textType() + ", " +
                "updated_at " + textType() + ", " +
                uniqueConstraint +
                ")");

        String ifNotExists = sqlite ? "IF NOT EXISTS" : "IF NOT EXISTS";
        executeIndex("CREATE INDEX " + ifNotExists + " idx_rec_history_type_date ON recommendation_history(lottery_type, recommend_date DESC)");
        executeIndex("CREATE INDEX " + ifNotExists + " idx_rec_history_type_strategy ON recommendation_history(lottery_type, strategy_name)");
    }

    // ===== 辅助方法 =====

    private void addColumnIfMissing(String tableName, List<String> existingColumns, String columnName, String definition) {
        if (existingColumns.contains(columnName)) return;
        try {
            jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
            log.info("已为表 [{}] 增加字段 [{}]", tableName, columnName);
        } catch (Exception e) {
            log.debug("添加字段 [{}.{}] 失败（可能已存在）: {}", tableName, columnName, e.getMessage());
        }
    }

    private void executeIndex(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception e) {
            log.debug("创建索引跳过（可能已存在）: {}", e.getMessage());
        }
    }
}
