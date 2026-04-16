CREATE TABLE IF NOT EXISTS lottery_result (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    lottery_type TEXT    NOT NULL,
    draw_num     TEXT    NOT NULL,
    draw_date    TEXT    NOT NULL,
    numbers      TEXT    NOT NULL,
    extra_info   TEXT,
    fetched_at   TEXT    DEFAULT (datetime('now','localtime')),
    created_at   TEXT,
    created_by   TEXT,
    updated_at   TEXT,
    updated_by   TEXT,
    UNIQUE(lottery_type, draw_num)
);

CREATE INDEX IF NOT EXISTS idx_type_date ON lottery_result(lottery_type, draw_date);
CREATE INDEX IF NOT EXISTS idx_type_draw ON lottery_result(lottery_type, draw_num);

CREATE TABLE IF NOT EXISTS fetch_history_task (
    task_id         TEXT PRIMARY KEY,
    trigger_source  TEXT    NOT NULL,
    type            TEXT    NOT NULL,
    scope           TEXT    NOT NULL,
    mode            TEXT    NOT NULL,
    status          TEXT    NOT NULL,
    current_type    TEXT,
    current_page    INTEGER DEFAULT 0,
    total_fetched   INTEGER DEFAULT 0,
    inserted        INTEGER DEFAULT 0,
    updated         INTEGER DEFAULT 0,
    completed_types INTEGER DEFAULT 0,
    total_types     INTEGER DEFAULT 0,
    error           TEXT,
    started_at      TEXT,
    finished_at     TEXT,
    created_at      TEXT,
    updated_at      TEXT
);

CREATE INDEX IF NOT EXISTS idx_fetch_history_task_started_at ON fetch_history_task(started_at DESC);
CREATE INDEX IF NOT EXISTS idx_fetch_history_task_status ON fetch_history_task(status);
CREATE INDEX IF NOT EXISTS idx_fetch_history_task_trigger_source ON fetch_history_task(trigger_source);
CREATE INDEX IF NOT EXISTS idx_fetch_history_task_type ON fetch_history_task(type);

CREATE TABLE IF NOT EXISTS fetch_history_detail (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    task_id       TEXT    NOT NULL,
    lottery_type  TEXT    NOT NULL,
    name          TEXT,
    scope         TEXT,
    status        TEXT    NOT NULL,
    current_page  INTEGER DEFAULT 0,
    total_fetched INTEGER DEFAULT 0,
    inserted      INTEGER DEFAULT 0,
    updated       INTEGER DEFAULT 0,
    error         TEXT,
    sort_order    INTEGER DEFAULT 0,
    created_at    TEXT,
    updated_at    TEXT,
    UNIQUE(task_id, lottery_type)
);

CREATE INDEX IF NOT EXISTS idx_fetch_history_detail_task_id ON fetch_history_detail(task_id);
CREATE INDEX IF NOT EXISTS idx_fetch_history_detail_type ON fetch_history_detail(lottery_type);
CREATE INDEX IF NOT EXISTS idx_fetch_history_detail_sort_order ON fetch_history_detail(task_id, sort_order);

CREATE TABLE IF NOT EXISTS recommendation_history (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    lottery_type          TEXT    NOT NULL,
    recommend_date        TEXT    NOT NULL,
    strategy_name         TEXT    NOT NULL,
    strategy_index        INTEGER NOT NULL,
    recommended_numbers   TEXT    NOT NULL,
    actual_numbers        TEXT,
    hit_main              INTEGER DEFAULT 0,
    hit_extra             INTEGER DEFAULT 0,
    created_at            TEXT,
    updated_at            TEXT,
    UNIQUE(lottery_type, recommend_date, strategy_index)
);

CREATE INDEX IF NOT EXISTS idx_rec_history_type_date ON recommendation_history(lottery_type, recommend_date DESC);
CREATE INDEX IF NOT EXISTS idx_rec_history_type_strategy ON recommendation_history(lottery_type, strategy_name);

-- ===== RBAC 权限五表 =====

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    username    TEXT NOT NULL UNIQUE,
    password    TEXT NOT NULL,
    nickname    TEXT,
    email       TEXT,
    phone       TEXT,
    sex         TEXT DEFAULT '2',
    avatar      TEXT,
    status      TEXT DEFAULT '0',
    del_flag    TEXT DEFAULT '0',
    login_ip    TEXT,
    login_date  TEXT,
    created_at  TEXT,
    updated_at  TEXT
);

CREATE INDEX IF NOT EXISTS idx_sys_user_username ON sys_user(username);

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    role_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    role_name   TEXT    NOT NULL,
    role_key    TEXT    NOT NULL,
    sort        INTEGER DEFAULT 0,
    status      TEXT    DEFAULT '0',
    remark      TEXT,
    created_at  TEXT,
    updated_at  TEXT
);

CREATE INDEX IF NOT EXISTS idx_sys_role_role_key ON sys_role(role_key);

-- 菜单/权限表
-- menu_type: M=目录 C=菜单 F=按钮
CREATE TABLE IF NOT EXISTS sys_menu (
    menu_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    menu_name   TEXT    NOT NULL,
    parent_id   INTEGER DEFAULT 0,
    order_num   INTEGER DEFAULT 0,
    path        TEXT,
    component   TEXT,
    menu_type   TEXT    DEFAULT 'C',
    perms       TEXT,
    icon        TEXT,
    visible     TEXT    DEFAULT '0',
    status      TEXT    DEFAULT '0',
    created_at  TEXT,
    updated_at  TEXT
);

CREATE INDEX IF NOT EXISTS idx_sys_menu_parent_id ON sys_menu(parent_id);

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id  INTEGER NOT NULL,
    role_id  INTEGER NOT NULL,
    PRIMARY KEY (user_id, role_id)
);

-- 角色-菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id  INTEGER NOT NULL,
    menu_id  INTEGER NOT NULL,
    PRIMARY KEY (role_id, menu_id)
);
