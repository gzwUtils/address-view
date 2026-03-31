-- 为 project 表添加所有者字段
-- 执行前请确保已备份数据库

ALTER TABLE project
ADD COLUMN owner_id VARCHAR(64) NULL COMMENT '所有者ID' AFTER description,
ADD COLUMN owner_name VARCHAR(128) NULL COMMENT '所有者名称' AFTER owner_id;

-- 添加索引以提高查询性能
ALTER TABLE project
ADD INDEX idx_owner_id (owner_id);
