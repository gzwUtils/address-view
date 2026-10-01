-- Apply once after init_project_table.sql. Back up the database first.
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

ALTER TABLE project ADD COLUMN owner_account_id BIGINT NULL;
CREATE INDEX idx_project_owner_account ON project(owner_account_id);
