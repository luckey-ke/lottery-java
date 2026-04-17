package com.lottery.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void migrate() {
        this.sqlite = detectSqlite();
        log.info("[SchemaMigration] 检测到数据库类型: {}", sqlite ? "SQLite" : "MySQL/Other");

        ensureAuditColumns("lottery_result");
        ensureFetchHistoryTaskTable();
        ensureFetchHistoryDetailTable();
        ensureFetchHistoryIndexes();
        ensureRecommendationHistoryTable();

        // ===== RBAC 五表 =====
        ensureSysUserTable();
        ensureSysRoleTable();
        ensureSysMenuTable();
        ensureSysUserRoleTable();
        ensureSysRoleMenuTable();

        // ===== 初始化数据 =====
        initDefaultData();
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
            try {
                return jdbcTemplate.query(
                        "PRAGMA table_info(" + tableName + ")",
                        (rs, rowNum) -> rs.getString("name")
                );
            } catch (Exception e) {
                return List.of();
            }
        } else {
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
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

    private String intType() {
        return sqlite ? "INTEGER" : "INT";
    }

    // ===== 通用方法 =====

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

    // ===== 原有表（保持不变）=====

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
                "current_page " + intType() + " DEFAULT 0, " +
                "total_fetched " + intType() + " DEFAULT 0, " +
                "inserted " + intType() + " DEFAULT 0, " +
                "updated " + intType() + " DEFAULT 0, " +
                "completed_types " + intType() + " DEFAULT 0, " +
                "total_types " + intType() + " DEFAULT 0, " +
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
        addColumnIfMissing("fetch_history_task", existing, "current_page", intType() + " DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existing, "total_fetched", intType() + " DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existing, "inserted", intType() + " DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existing, "updated", intType() + " DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existing, "completed_types", intType() + " DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existing, "total_types", intType() + " DEFAULT 0");
        addColumnIfMissing("fetch_history_task", existing, "error", "TEXT");
        addColumnIfMissing("fetch_history_task", existing, "started_at", textType());
        addColumnIfMissing("fetch_history_task", existing, "finished_at", textType());
        addColumnIfMissing("fetch_history_task", existing, "created_at", textType());
        addColumnIfMissing("fetch_history_task", existing, "updated_at", textType());
    }

    private void ensureFetchHistoryDetailTable() {
        String idCol = "id " + autoIncrementColumn();
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
                "current_page " + intType() + " DEFAULT 0, " +
                "total_fetched " + intType() + " DEFAULT 0, " +
                "inserted " + intType() + " DEFAULT 0, " +
                "updated " + intType() + " DEFAULT 0, " +
                "error TEXT, " +
                "sort_order " + intType() + " DEFAULT 0, " +
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
        addColumnIfMissing("fetch_history_detail", existing, "current_page", intType() + " DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existing, "total_fetched", intType() + " DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existing, "inserted", intType() + " DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existing, "updated", intType() + " DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existing, "error", "TEXT");
        addColumnIfMissing("fetch_history_detail", existing, "sort_order", intType() + " DEFAULT 0");
        addColumnIfMissing("fetch_history_detail", existing, "created_at", textType());
        addColumnIfMissing("fetch_history_detail", existing, "updated_at", textType());
    }

    private void ensureFetchHistoryIndexes() {
        String ifNotExists = "IF NOT EXISTS";
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
                "strategy_index " + intType() + " NOT NULL, " +
                "recommended_numbers TEXT NOT NULL, " +
                "actual_numbers TEXT, " +
                "hit_main " + intType() + " DEFAULT 0, " +
                "hit_extra " + intType() + " DEFAULT 0, " +
                "created_at " + textType() + ", " +
                "updated_at " + textType() + ", " +
                uniqueConstraint +
                ")");

        String ifNotExists = "IF NOT EXISTS";
        executeIndex("CREATE INDEX " + ifNotExists + " idx_rec_history_type_date ON recommendation_history(lottery_type, recommend_date DESC)");
        executeIndex("CREATE INDEX " + ifNotExists + " idx_rec_history_type_strategy ON recommendation_history(lottery_type, strategy_name)");
    }

    // ===== RBAC 五表 =====

    private void ensureSysUserTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_user (
                    id          %s,
                    username    %s NOT NULL UNIQUE,
                    password    %s NOT NULL,
                    nickname    %s,
                    email       %s,
                    phone       %s,
                    sex         %s DEFAULT '2',
                    avatar      %s,
                    status      %s DEFAULT '0',
                    del_flag    %s DEFAULT '0',
                    login_ip    %s,
                    login_date  %s,
                    created_at  %s,
                    updated_at  %s
                )
                """.formatted(
                        autoIncrementColumn(),
                        textType(), textType(), textType(), textType(), textType(),
                        textType(), textType(), textType(), textType(),
                        textType(), textType(), textType(), textType()
                ));
            executeIndex("CREATE INDEX IF NOT EXISTS idx_sys_user_username ON sys_user(username)");
            log.info("[SchemaMigration] sys_user 表就绪");

            // 迁移旧字段：如果有 role 列，保留数据但不再使用
            List<String> existing = getExistingColumns("sys_user");
            if (existing.contains("role") && !existing.contains("del_flag")) {
                addColumnIfMissing("sys_user", existing, "del_flag", textType() + " DEFAULT '0'");
            }
            addColumnIfMissing("sys_user", existing, "nickname", textType());
            addColumnIfMissing("sys_user", existing, "email", textType());
            addColumnIfMissing("sys_user", existing, "phone", textType());
            addColumnIfMissing("sys_user", existing, "sex", textType() + " DEFAULT '2'");
            addColumnIfMissing("sys_user", existing, "avatar", textType());
            addColumnIfMissing("sys_user", existing, "status", textType() + " DEFAULT '0'");
            addColumnIfMissing("sys_user", existing, "del_flag", textType() + " DEFAULT '0'");
            addColumnIfMissing("sys_user", existing, "login_ip", textType());
            addColumnIfMissing("sys_user", existing, "login_date", textType());
        } catch (Exception e) {
            log.warn("[SchemaMigration] sys_user 表处理失败: {}", e.getMessage());
        }
    }

    private void ensureSysRoleTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_role (
                    role_id     %s,
                    role_name   %s NOT NULL,
                    role_key    %s NOT NULL,
                    sort        %s DEFAULT 0,
                    status      %s DEFAULT '0',
                    remark      %s,
                    created_at  %s,
                    updated_at  %s
                )
                """.formatted(
                        autoIncrementColumn(),
                        textType(), textType(), intType(),
                        textType(), textType(), textType(), textType()
                ));
            executeIndex("CREATE INDEX IF NOT EXISTS idx_sys_role_role_key ON sys_role(role_key)");
            log.info("[SchemaMigration] sys_role 表就绪");
        } catch (Exception e) {
            log.warn("[SchemaMigration] sys_role 表创建失败: {}", e.getMessage());
        }
    }

    private void ensureSysMenuTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_menu (
                    menu_id     %s,
                    menu_name   %s NOT NULL,
                    parent_id   %s DEFAULT 0,
                    order_num   %s DEFAULT 0,
                    path        %s,
                    component   %s,
                    menu_type   %s DEFAULT 'C',
                    perms       %s,
                    icon        %s,
                    visible     %s DEFAULT '0',
                    status      %s DEFAULT '0',
                    created_at  %s,
                    updated_at  %s
                )
                """.formatted(
                        autoIncrementColumn(),
                        textType(), intType(), intType(),
                        textType(), textType(), textType(),
                        textType(), textType(), textType(), textType(),
                        textType(), textType()
                ));
            executeIndex("CREATE INDEX IF NOT EXISTS idx_sys_menu_parent_id ON sys_menu(parent_id)");
            // 迁移：添加 menu_location 列
            List<String> menuCols = getExistingColumns("sys_menu");
            addColumnIfMissing("sys_menu", menuCols, "menu_location", textType() + " DEFAULT 'admin'");
            // 迁移已有数据：前台页面设为 frontend
            migrateMenuLocations();
            log.info("[SchemaMigration] sys_menu 表就绪");
        } catch (Exception e) {
            log.warn("[SchemaMigration] sys_menu 表创建失败: {}", e.getMessage());
        }
    }

    private void ensureSysUserRoleTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_user_role (
                    user_id  %s NOT NULL,
                    role_id  %s NOT NULL,
                    PRIMARY KEY (user_id, role_id)
                )
                """.formatted(intType(), intType()));
            log.info("[SchemaMigration] sys_user_role 表就绪");
        } catch (Exception e) {
            log.warn("[SchemaMigration] sys_user_role 表创建失败: {}", e.getMessage());
        }
    }

    private void ensureSysRoleMenuTable() {
        try {
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS sys_role_menu (
                    role_id  %s NOT NULL,
                    menu_id  %s NOT NULL,
                    PRIMARY KEY (role_id, menu_id)
                )
                """.formatted(intType(), intType()));
            log.info("[SchemaMigration] sys_role_menu 表就绪");
        } catch (Exception e) {
            log.warn("[SchemaMigration] sys_role_menu 表创建失败: {}", e.getMessage());
        }
    }

    private void migrateMenuLocations() {
        try {
            // 1. 前台页面设为 frontend
            String[] frontendPaths = {"dashboard", "analysis", "trend", "recommend"};
            for (String p : frontendPaths) {
                jdbcTemplate.update(
                        "UPDATE sys_menu SET menu_location = 'frontend' WHERE path = ? AND (menu_location IS NULL OR menu_location = 'admin')",
                        p
                );
            }

            // 2. 修正拉取历史的 path
            jdbcTemplate.update(
                    "UPDATE sys_menu SET path = 'history' WHERE menu_name = '拉取历史' AND path = 'fetch-history'"
            );

            // 3. 确保 admin 位置的菜单存在（如果还没有的话）
            int adminMenuCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM sys_menu WHERE menu_location = 'admin' AND menu_type = 'C'",
                    Integer.class
            );
            if (adminMenuCount == 0) {
                String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                // 创建后台菜单项
                String[][] adminMenus = {
                    {"管理后台", "", "Admin", "C", "1", "setting"},
                    {"拉取历史", "history", "FetchHistory", "C", "2", "list"},
                    {"用户管理", "user", "Users", "C", "3", "user"},
                    {"角色管理", "role", "Roles", "C", "4", "peoples"},
                    {"菜单管理", "menu", "Menus", "C", "5", "tree-table"},
                };
                for (String[] m : adminMenus) {
                    jdbcTemplate.update(
                            "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, icon, visible, status, menu_location, created_at, updated_at) VALUES (?, 0, ?, ?, ?, ?, ?, '0', '0', 'admin', ?, ?)",
                            m[0], Integer.parseInt(m[3]), m[1], m[2], m[4], m[5], now, now
                    );
                }
                // 关联到管理员角色
                Integer adminRoleId = jdbcTemplate.queryForObject("SELECT role_id FROM sys_role WHERE role_key = 'admin'", Integer.class);
                if (adminRoleId != null) {
                    List<Integer> newMenuIds = jdbcTemplate.queryForList(
                            "SELECT menu_id FROM sys_menu WHERE menu_location = 'admin' AND menu_type = 'C' AND parent_id = 0 ORDER BY menu_id DESC LIMIT 5",
                            Integer.class
                    );
                    for (Integer mid : newMenuIds) {
                        try {
                            jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", adminRoleId, mid);
                        } catch (Exception ignored) {}
                    }
                }
            }

            // 4. 修复"数据管理"：应为目录类型(M)而非菜单类型(C)
            //    容器节点不应出现在侧栏导航中，其子菜单通过递归收集
            jdbcTemplate.update(
                    "UPDATE sys_menu SET menu_type = 'M' WHERE menu_name = '数据管理' AND menu_type = 'C'"
            );

            log.info("[SchemaMigration] menu_location 数据迁移完成");
        } catch (Exception e) {
            log.debug("[SchemaMigration] menu_location 迁移跳过: {}", e.getMessage());
        }
    }

    // ===== 初始化默认数据 =====

    private void initDefaultData() {
        try {
            // 1. 默认角色
            int roleCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM sys_role", Integer.class);
            if (roleCount == 0) {
                String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                jdbcTemplate.update(
                        "INSERT INTO sys_role (role_name, role_key, sort, status, remark, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        "管理员", "admin", 1, "0", "系统管理员（拥有全部权限）", now, now
                );
                jdbcTemplate.update(
                        "INSERT INTO sys_role (role_name, role_key, sort, status, remark, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        "普通用户", "user", 2, "0", "普通用户（仅查看权限）", now, now
                );
                log.info("[DataInit] 默认角色已初始化: admin, user");
            }

            // 2. 默认菜单和权限
            int menuCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM sys_menu", Integer.class);
            if (menuCount == 0) {
                String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // M=目录 C=菜单 F=按钮
                // parent_id=0 为顶级

                // --- 系统管理 (目录) ---
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "系统管理", 0, 1, "system", null, "M", null, "setting", "0", "0", now, now
                );
                int sysMenuId = jdbcTemplate.queryForObject("SELECT MAX(menu_id) FROM sys_menu", Integer.class);

                // 系统管理 -> 用户管理 (菜单)
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "用户管理", sysMenuId, 1, "user", "system/user/User", "C", null, "user", "0", "0", now, now
                );
                int userMenuId = jdbcTemplate.queryForObject("SELECT MAX(menu_id) FROM sys_menu", Integer.class);

                // 用户管理下的按钮
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "用户查询", userMenuId, 1, "#", null, "F", "system:user:list", null, "0", "0", now, now
                );
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "用户新增", userMenuId, 2, "#", null, "F", "system:user:add", null, "0", "0", now, now
                );
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "用户修改", userMenuId, 3, "#", null, "F", "system:user:edit", null, "0", "0", now, now
                );
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "用户删除", userMenuId, 4, "#", null, "F", "system:user:remove", null, "0", "0", now, now
                );

                // 系统管理 -> 角色管理 (菜单)
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "角色管理", sysMenuId, 2, "role", "system/role/Role", "C", null, "peoples", "0", "0", now, now
                );
                int roleMenuId = jdbcTemplate.queryForObject("SELECT MAX(menu_id) FROM sys_menu", Integer.class);

                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "角色查询", roleMenuId, 1, "#", null, "F", "system:role:list", null, "0", "0", now, now
                );
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "角色新增", roleMenuId, 2, "#", null, "F", "system:role:add", null, "0", "0", now, now
                );
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "角色修改", roleMenuId, 3, "#", null, "F", "system:role:edit", null, "0", "0", now, now
                );
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "角色删除", roleMenuId, 4, "#", null, "F", "system:role:remove", null, "0", "0", now, now
                );

                // 系统管理 -> 菜单管理 (菜单)
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "菜单管理", sysMenuId, 3, "menu", "system/menu/Menu", "C", null, "tree-table", "0", "0", now, now
                );
                int menuMenuId = jdbcTemplate.queryForObject("SELECT MAX(menu_id) FROM sys_menu", Integer.class);

                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "菜单查询", menuMenuId, 1, "#", null, "F", "system:menu:list", null, "0", "0", now, now
                );
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "菜单新增", menuMenuId, 2, "#", null, "F", "system:menu:add", null, "0", "0", now, now
                );
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "菜单修改", menuMenuId, 3, "#", null, "F", "system:menu:edit", null, "0", "0", now, now
                );
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "菜单删除", menuMenuId, 4, "#", null, "F", "system:menu:remove", null, "0", "0", now, now
                );

                // --- 数据总览 (目录) ---
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, menu_location, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "数据总览", 0, 2, "dashboard", "Dashboard", "C", "lottery:dashboard:list", "dashboard", "0", "0", "frontend", now, now
                );

                // --- 数据管理 (目录) ---
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "数据管理", 0, 3, "admin", "Admin", "C", null, "data-board", "0", "0", now, now
                );
                int dataMenuId = jdbcTemplate.queryForObject("SELECT MAX(menu_id) FROM sys_menu", Integer.class);

                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "数据拉取", dataMenuId, 1, "#", null, "F", "lottery:lottery:fetch", null, "0", "0", now, now
                );
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "拉取历史", dataMenuId, 2, "history", "FetchHistory", "C", "lottery:fetch:history", "history", "0", "0", now, now
                );

                // --- 统计分析 ---
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, menu_location, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "统计分析", 0, 4, "analysis", "Analysis", "C", "lottery:analysis:list", "chart", "0", "0", "frontend", now, now
                );

                // --- 趋势分析 ---
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, menu_location, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "趋势分析", 0, 5, "trend", "Trend", "C", "lottery:trend:list", "trend-charts", "0", "0", "frontend", now, now
                );

                // --- 号码推荐 ---
                jdbcTemplate.update(
                        "INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, visible, status, menu_location, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        "号码推荐", 0, 6, "recommend", "Recommend", "C", "lottery:recommend:list", "magic-stick", "0", "0", "frontend", now, now
                );

                log.info("[DataInit] 默认菜单和权限已初始化（共 ~30 条）");
            }

            // 3. 默认 admin 用户（如果不存在）
            int userCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM sys_user WHERE username = 'admin'", Integer.class);
            if (userCount == 0) {
                String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                // admin / admin123 (BCrypt)
                String bcryptPw = "$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2";
                jdbcTemplate.update(
                        "INSERT INTO sys_user (username, password, nickname, status, del_flag, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        "admin", bcryptPw, "管理员", "0", "0", now, now
                );
                // 给 admin 用户分配管理员角色
                int adminUserId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM sys_user", Integer.class);
                int adminRoleId = jdbcTemplate.queryForObject("SELECT role_id FROM sys_role WHERE role_key = 'admin'", Integer.class);
                jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", adminUserId, adminRoleId);

                // 管理员角色关联所有菜单
                List<Integer> allMenuIds = jdbcTemplate.queryForList("SELECT menu_id FROM sys_menu", Integer.class);
                for (Integer mid : allMenuIds) {
                    jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", adminRoleId, mid);
                }

                log.info("[DataInit] 默认管理员账号已创建: admin / admin123");
            }

            // 4. 迁移旧数据：如果有用户用旧 role 字段但没有 sys_user_role 记录
            try {
                List<Object[]> oldAdmins = jdbcTemplate.query(
                        "SELECT id FROM sys_user WHERE role = 'ADMIN' AND del_flag = '0'",
                        (rs, rowNum) -> new Object[]{rs.getInt("id")}
                );
                int adminRoleId = jdbcTemplate.queryForObject("SELECT role_id FROM sys_role WHERE role_key = 'admin'", Integer.class);
                int userRoleId = jdbcTemplate.queryForObject("SELECT role_id FROM sys_role WHERE role_key = 'user'", Integer.class);
                for (Object[] row : oldAdmins) {
                    int uid = (int) row[0];
                    int cnt = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM sys_user_role WHERE user_id = ?", Integer.class, uid);
                    if (cnt == 0) {
                        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", uid, adminRoleId);
                    }
                }
                List<Object[]> oldUsers = jdbcTemplate.query(
                        "SELECT id FROM sys_user WHERE role = 'USER' AND del_flag = '0'",
                        (rs, rowNum) -> new Object[]{rs.getInt("id")}
                );
                for (Object[] row : oldUsers) {
                    int uid = (int) row[0];
                    int cnt = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM sys_user_role WHERE user_id = ?", Integer.class, uid);
                    if (cnt == 0) {
                        jdbcTemplate.update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", uid, userRoleId);
                    }
                }
                log.info("[DataInit] 旧角色数据迁移完成");
            } catch (Exception e) {
                log.debug("[DataInit] 旧数据迁移跳过: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.warn("[DataInit] 初始化默认数据失败: {}", e.getMessage());
        }
    }
}
