-- Apply once after migration_guest_identity.sql. Back up the database first.
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
