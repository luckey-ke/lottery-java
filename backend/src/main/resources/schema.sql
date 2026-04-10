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

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    username    TEXT    NOT NULL UNIQUE,
    password    TEXT    NOT NULL,
    role        TEXT    NOT NULL DEFAULT 'USER',
    nickname    TEXT,
    created_at  TEXT,
    updated_at  TEXT
);

CREATE INDEX IF NOT EXISTS idx_sys_user_username ON sys_user(username);
