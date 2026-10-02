-- Full schema for a fresh protal database. Run against an existing empty protal database.
-- Spring Boot also runs this file on startup; all DDL and seed data must remain repeatable.
-- This file deliberately does not drop tables.

-- Recoverable guest identity

CREATE TABLE IF NOT EXISTS community_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  public_id VARCHAR(20) NOT NULL UNIQUE,
  nickname VARCHAR(20) NOT NULL UNIQUE,
  recovery_hash CHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'active',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS community_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  account_id BIGINT NOT NULL,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME NOT NULL,
  revoked_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_account (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS community_rate_limit (
  action_key VARCHAR(128) NOT NULL,
  window_start DATETIME NOT NULL,
  hit_count INT NOT NULL,
  PRIMARY KEY (action_key, window_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Projects

CREATE TABLE IF NOT EXISTS project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '项目ID',
    project_name VARCHAR(255) NOT NULL COMMENT '项目名称',
    short_name VARCHAR(64) NOT NULL COMMENT '名称简写',
    platform_url VARCHAR(500) NOT NULL COMMENT '平台访问地址',
    background_image VARCHAR(500) NULL COMMENT '背景图URL',
    category VARCHAR(128) NOT NULL COMMENT '项目分类',
    type VARCHAR(64) NULL COMMENT '项目类型（项目/研发）',
    description VARCHAR(1000) NULL COMMENT '项目描述',
    owner_id VARCHAR(64) NULL COMMENT '所有者ID（用户标识）',
    owner_name VARCHAR(128) NULL COMMENT '所有者名称',
    owner_account_id BIGINT NULL COMMENT '可恢复访客账户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_category (category),
    KEY idx_owner_id (owner_id),
    KEY idx_project_owner_account (owner_account_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目导航表';

-- Admin-configured external project sources, separate from member-owned projects.
CREATE TABLE IF NOT EXISTS external_source (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    feed_url VARCHAR(500) NULL,
    query_text VARCHAR(100) NULL,
    period_days INT NOT NULL DEFAULT 7,
    min_stars INT NOT NULL DEFAULT 10,
    max_items INT NOT NULL DEFAULT 5,
    interval_hours INT NOT NULL DEFAULT 168,
    enabled TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    last_status VARCHAR(16) NULL,
    last_error VARCHAR(255) NULL,
    last_run_at DATETIME NULL,
    next_run_at DATETIME NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_source_schedule (enabled, deleted, next_run_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员配置的站外项目来源';

INSERT IGNORE INTO external_source
  (code, display_name, source_type, period_days, min_stars, max_items, interval_hours, enabled, next_run_at)
VALUES ('github', 'GitHub 新项目', 'github_search', 7, 10, 5, 168, 1, NULL);

CREATE TABLE IF NOT EXISTS external_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_platform VARCHAR(32) NOT NULL,
    source_repo_id BIGINT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    source_url VARCHAR(500) NOT NULL,
    description VARCHAR(1000) NULL,
    language VARCHAR(80) NULL,
    license_spdx VARCHAR(80) NOT NULL,
    star_count INT NOT NULL DEFAULT 0,
    fork_count INT NOT NULL DEFAULT 0,
    repo_created_at DATETIME NOT NULL,
    synced_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    display_rank INT NOT NULL,
    featured TINYINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_external_source_repo (source_platform, source_repo_id),
    KEY idx_external_featured (featured, display_rank)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外部开源项目精选';

-- Portal content and activity

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

-- Community discussions and moderation

CREATE TABLE IF NOT EXISTS community_board (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(40) NOT NULL,
  description VARCHAR(255) NOT NULL,
  sort_order INT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'visible'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO community_board(code,name,description,sort_order) VALUES
('project-share','项目分享','分享项目与使用经验',10),
('tech-talk','技术交流','讨论实现与排障',20),
('lounge','闲聊','团队日常交流',30);

CREATE TABLE IF NOT EXISTS community_topic (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  board_id BIGINT NOT NULL,
  author_account_id BIGINT NOT NULL,
  project_id BIGINT NULL,
  title VARCHAR(120) NOT NULL,
  body TEXT NOT NULL,
  reply_count INT NOT NULL DEFAULT 0,
  next_floor INT NOT NULL DEFAULT 2,
  last_reply_time DATETIME NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'visible',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_board_activity (board_id,last_reply_time,id),
  KEY idx_project (project_id),
  KEY idx_author (author_account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS community_reply (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  topic_id BIGINT NOT NULL,
  author_account_id BIGINT NOT NULL,
  reply_to_id BIGINT NULL,
  floor_no INT NOT NULL,
  body TEXT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'visible',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_topic_floor (topic_id,floor_no),
  KEY idx_topic_time (topic_id,create_time,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS community_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_account_id BIGINT NOT NULL,
  target_type VARCHAR(16) NOT NULL,
  target_id BIGINT NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'open',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_status_time (status,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS community_admin_session (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token_hash CHAR(64) NOT NULL UNIQUE,
  expires_at DATETIME NOT NULL,
  revoked_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Initial portal content

INSERT INTO portal_resource
(resource_code, kind, title, name, excerpt, description, category, type, owner, author, meta, resource_date, status, version, updated_at, entry_url, cover_image, content_body, tags, capabilities, sort_order, deleted)
VALUES
('article-order-migration', 'article', '订单中心迁移复盘', NULL, '记录从单体迁移到分层服务后的流量切换、灰度策略与故障回收方式。', NULL, '项目复盘', NULL, NULL, '高致维', NULL, '2026-03-22', NULL, NULL, NULL, NULL, 'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80', '# 订单中心迁移复盘\n\n## 背景\n\n订单中心从单体迁移到分层服务后，最关键的问题不是代码，而是**流量切换的节奏**。\n\n## 这次复盘关注什么\n\n- 灰度比例怎么设计\n- 回滚链路是否足够短\n- 监控是否覆盖核心指标\n\n> 真正决定迁移成败的，往往不是方案多先进，而是回退是否足够简单。', '订单,迁移,灰度', NULL, 10, 0),
('article-spring-idempotent', 'article', 'Spring Boot 接口幂等设计笔记', NULL, '梳理令牌、去重表和消息补偿三种常见幂等实现方式及适用边界。', NULL, '最佳实践', NULL, NULL, '架构组', NULL, '2026-03-18', NULL, NULL, NULL, NULL, 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=1200&q=80', '# Spring Boot 接口幂等设计笔记\n\n## 常见方案\n\n1. 前端请求令牌\n2. 服务端去重表\n3. 消息补偿与状态机\n\n### 经验结论\n\n**不要只谈幂等，要先定义业务边界。**\n\n- 创建类接口适合令牌或唯一索引\n- 回调类接口适合状态机校验\n- 异步链路要配合补偿策略', 'Java,Spring Boot,幂等', NULL, 20, 0),
('article-db-hot-row', 'article', '一次数据库热点行争用排查', NULL, '从监控、慢 SQL、线程栈和业务重试策略四个维度拆解问题。', NULL, '故障记录', NULL, NULL, 'SRE 团队', NULL, '2026-03-11', NULL, NULL, NULL, NULL, 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=1200&q=80', '# 一次数据库热点行争用排查\n\n## 现象\n\n同一时段内接口 RT 激增，线程池开始堆积。\n\n## 排查路径\n\n- 先看监控趋势\n- 再看慢 SQL\n- 再拉线程栈\n- 最后回到业务重试逻辑\n\n## 最后定位\n\n热点行被高频更新，重试机制又放大了冲突。', 'MySQL,排障,性能', NULL, 30, 0),
('ai-release-assistant', 'ai', NULL, '研发发布助手', NULL, '用于生成发布检查单、回滚建议和变更摘要。', NULL, 'Agent', 'DevOps Team', NULL, NULL, NULL, 'online', 'v2.3.1', '2026-03-26', 'https://ai.example.com/release-assistant', NULL, NULL, '发布,变更,自动摘要', '发布摘要,回滚检查,风险提示', 40, 0),
('ai-api-design-skill', 'ai', NULL, '接口设计 Skill 包', NULL, '面向 RESTful 接口评审与字段规范的团队标准技能集。', NULL, 'Skill', 'Architecture Group', NULL, NULL, NULL, 'online', 'v1.8.0', '2026-03-21', 'https://ai.example.com/skills/api-design', NULL, NULL, 'API,评审,标准化', '字段评审,接口规范,错误码建议', 50, 0),
('ai-log-mcp', 'ai', NULL, '日志检索连接器', NULL, '聚合日志、监控与知识库资源，为故障分析 Agent 提供上下文。', NULL, 'MCP', 'SRE Team', NULL, NULL, NULL, 'trial', 'v0.9.4', '2026-03-18', 'https://ai.example.com/mcp/logs', NULL, NULL, '日志,监控,上下文', '日志聚合,监控读取,知识库检索', 60, 0),
('ai-weekly-openclaw', 'ai', NULL, '周报编排流', NULL, '自动拉取项目动态、合并知识沉淀和公告，生成周报初稿。', NULL, 'OpenClaw', 'PMO', NULL, NULL, NULL, 'draft', 'v0.6.2', '2026-03-15', 'https://ai.example.com/workflows/weekly-report', NULL, NULL, '编排,周报,自动化', '信息汇总,周报生成,流程编排', 70, 0),
('life-night-run', 'life', '本周五 18:30 团队夜跑', NULL, NULL, '下班后从园区北门集合，欢迎研发、产品、测试一起参加。', NULL, '公告', NULL, NULL, '生活社群', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 80, 0),
('life-release-window', 'life', '清明节前发布窗口收紧', NULL, NULL, '核心系统发布需提前一天完成风险评估与值班确认。', NULL, '提醒', NULL, NULL, '运维规范', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 90, 0),
('life-digital-magazine', 'life', '把门户做成团队数字杂志', NULL, NULL, '除了效率，门户也应该记录团队故事、实践和真实的日常节奏。', NULL, '灵感', NULL, NULL, '设计建议', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 100, 0)
ON DUPLICATE KEY UPDATE
title = VALUES(title),
name = VALUES(name),
excerpt = VALUES(excerpt),
description = VALUES(description),
category = VALUES(category),
type = VALUES(type),
owner = VALUES(owner),
author = VALUES(author),
meta = VALUES(meta),
resource_date = VALUES(resource_date),
status = VALUES(status),
version = VALUES(version),
updated_at = VALUES(updated_at),
entry_url = VALUES(entry_url),
cover_image = VALUES(cover_image),
content_body = VALUES(content_body),
tags = VALUES(tags),
capabilities = VALUES(capabilities),
sort_order = VALUES(sort_order),
deleted = VALUES(deleted);

INSERT INTO operation_log (module, action, target_type, target_name, operator_name, detail, create_time)
SELECT 'content', 'init', 'article', '订单中心迁移复盘', 'system', '初始化门户内容', NOW()
WHERE NOT EXISTS (SELECT 1 FROM operation_log);
