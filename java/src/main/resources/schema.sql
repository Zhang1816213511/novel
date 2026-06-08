-- ============================================================
-- NovelWriter 数据库初始化脚本
-- 首次启动自动执行（CREATE TABLE IF NOT EXISTS），已存在的表不会重复创建
-- ============================================================

-- ─── 作品表 ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS novel_entity (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,  -- 作品 ID（自增主键）
    title           TEXT    NOT NULL DEFAULT '',         -- 作品标题
    workspace_dir   TEXT,                                -- md 文件存放目录（用户创建时指定）
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP   -- 更新时间
);

-- ─── 章节表 ─────────────────────────────────────────────────
-- 章节的梗概和正文保存在 md 文件（workspace_dir/ch_NNN/summary.md / content.md）
CREATE TABLE IF NOT EXISTS chapter (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,  -- 章节 ID（自增主键）
    novel_id        INTEGER NOT NULL,                    -- 所属作品 ID
    chapter_number  INTEGER NOT NULL,                    -- 章节号（从 1 开始递增）
    title           TEXT    DEFAULT '',                   -- 章节标题
    md_dir          TEXT,                                -- md 文件目录相对路径（如 "ch_001"）
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP   -- 更新时间
);

-- ─── 模型配置表 ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS model_config (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,  -- 配置 ID（自增主键）
    name            TEXT    NOT NULL,                     -- 配置名称（用户自定义）
    provider        TEXT    NOT NULL,                     -- 提供商（ollama / openai）
    base_url        TEXT,                                -- API 地址（如 http://localhost:11434）
    model_name      TEXT    NOT NULL,                     -- 模型名（如 qwen3-max / gpt-4o）
    api_key         TEXT,                                -- API 密钥（Ollama 通常不需要）
    options         TEXT,                                -- 生成参数 JSON（temperature / maxTokens 等）
    enabled         INTEGER DEFAULT 1,                   -- 是否启用（1=启用 0=禁用）
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP   -- 更新时间
);

-- ─── 系统配置表 ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS system_config (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,  -- 配置 ID（自增主键）
    config_key      TEXT NOT NULL UNIQUE,                -- 配置键（如 app.workspace_root）
    config_value    TEXT,                                -- 配置值
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,  -- 创建时间
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP   -- 更新时间
);
