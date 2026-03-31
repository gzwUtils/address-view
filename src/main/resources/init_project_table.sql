-- Project 表完整定义
-- 如果表已存在，此脚本会跳过创建

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
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_category (category),
    KEY idx_owner_id (owner_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目导航表';

-- 示例数据（可选）
-- INSERT INTO project (project_name, short_name, platform_url, background_image, category, type, description, owner_id, owner_name)
-- VALUES
-- ('订单中心', 'Order', 'https://order.example.com', 'https://example.com/bg1.jpg', '默认', '项目', '订单管理系统', 'user-abc123', '用户ABC123');
