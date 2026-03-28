CREATE TABLE IF NOT EXISTS portal_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    resource_code VARCHAR(64) NOT NULL COMMENT '资源唯一编码',
    kind VARCHAR(32) NOT NULL COMMENT 'article/ai/life',
    title VARCHAR(255) NULL,
    name VARCHAR(255) NULL,
    excerpt VARCHAR(1000) NULL,
    description VARCHAR(1000) NULL,
    category VARCHAR(128) NULL,
    type VARCHAR(128) NULL,
    owner VARCHAR(128) NULL,
    author VARCHAR(128) NULL,
    meta VARCHAR(255) NULL,
    resource_date VARCHAR(32) NULL,
    status VARCHAR(32) NULL,
    version VARCHAR(64) NULL,
    updated_at VARCHAR(32) NULL,
    entry_url VARCHAR(500) NULL,
    cover_image VARCHAR(500) NULL COMMENT '封面图',
    content_body LONGTEXT NULL COMMENT 'Markdown正文',
    tags VARCHAR(1000) NULL,
    capabilities VARCHAR(1000) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_resource_code (resource_code),
    KEY idx_kind (kind),
    KEY idx_type (type),
    KEY idx_status (status),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户统一资源表';


CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module VARCHAR(64) NOT NULL COMMENT '模块 project/content/ai',
    action VARCHAR(32) NOT NULL COMMENT 'create/update/delete',
    target_type VARCHAR(64) NULL COMMENT 'article/ai/life/project',
    target_name VARCHAR(255) NULL COMMENT '目标名称',
    operator_name VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '操作人',
    detail VARCHAR(500) NULL COMMENT '操作描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_module (module),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台操作日志';

CREATE TABLE IF NOT EXISTS recent_view_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_id VARCHAR(64) NOT NULL COMMENT '匿名客户端标识',
    kind VARCHAR(32) NOT NULL COMMENT 'project/article/ai/life',
    target_id BIGINT NOT NULL COMMENT '目标资源ID',
    title VARCHAR(255) NOT NULL COMMENT '展示标题',
    subtitle VARCHAR(255) NULL COMMENT '展示副标题',
    view_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近浏览时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_client_target (client_id, kind, target_id),
    KEY idx_client_view_time (client_id, view_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='最近浏览历史';

CREATE TABLE IF NOT EXISTS user_growth_capsule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL COMMENT '用户标识',
    capsule_name VARCHAR(128) NOT NULL COMMENT '落地舱名称',
    tagline VARCHAR(255) NULL COMMENT '标语',
    ai_brief VARCHAR(500) NULL COMMENT 'AI 洞察摘要',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_growth_capsule (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户灵感落地舱';

CREATE TABLE IF NOT EXISTS user_growth_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    capsule_id BIGINT NOT NULL COMMENT '落地舱ID',
    source_kind VARCHAR(32) NOT NULL COMMENT 'project/article/ai/life',
    source_id BIGINT NOT NULL COMMENT '来源内容ID',
    title VARCHAR(255) NOT NULL COMMENT '标题',
    subtitle VARCHAR(255) NULL COMMENT '副标题',
    note VARCHAR(1000) NULL COMMENT '用户备注',
    action_plan TEXT NULL COMMENT '行动计划',
    value_summary VARCHAR(1000) NULL COMMENT '价值总结',
    first_step VARCHAR(500) NULL COMMENT '第一步建议',
    seven_day_plan VARCHAR(1000) NULL COMMENT '七天计划',
    status VARCHAR(32) NOT NULL DEFAULT 'todo' COMMENT 'todo/in_progress/done',
    expected_minutes INT NOT NULL DEFAULT 20 COMMENT '预估投入分钟',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_capsule_source (capsule_id, source_kind, source_id),
    KEY idx_capsule_status (capsule_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户落地舱行动卡';

CREATE TABLE IF NOT EXISTS user_growth_checkin (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL COMMENT '行动卡ID',
    content VARCHAR(1000) NOT NULL COMMENT '打卡内容',
    mood VARCHAR(32) NULL COMMENT '状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_item_create_time (item_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户落地舱打卡记录';
