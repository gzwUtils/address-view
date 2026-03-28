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
