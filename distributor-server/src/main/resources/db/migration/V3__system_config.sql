CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_group VARCHAR(64) NOT NULL COMMENT '分组: OSS/OAUTH_YOUTUBE/OAUTH_TIKTOK/SECURITY',
    config_key VARCHAR(128) NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值（敏感值加密存储）',
    encrypted TINYINT UNSIGNED DEFAULT 0 COMMENT '是否加密 0-明文 1-AES加密',
    description VARCHAR(256) COMMENT '说明',
    created_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE INDEX uk_group_key (config_group, config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

INSERT INTO system_config (config_group, config_key, encrypted, description) VALUES
('OSS', 'endpoint', 0, 'OSS公网Endpoint'),
('OSS', 'internal_endpoint', 0, 'OSS内网Endpoint'),
('OSS', 'bucket', 0, 'Bucket名称'),
('OSS', 'region', 0, '地域'),
('OSS', 'access_key_id', 1, 'AK（加密存储）'),
('OSS', 'access_key_secret', 1, 'SK（加密存储）'),
('OSS', 'sts_role_arn', 0, 'STS角色ARN'),
('OSS', 'sts_endpoint', 0, 'STS Endpoint'),
('OSS', 'sts_duration_seconds', 0, 'STS凭证有效期(秒)'),
('OSS', 'upload_prefix', 0, '上传路径前缀'),
('OAUTH_YOUTUBE', 'client_id', 0, 'YouTube ClientId'),
('OAUTH_YOUTUBE', 'client_secret', 1, 'YouTube Secret（加密）'),
('OAUTH_YOUTUBE', 'redirect_uri', 0, '回调地址'),
('OAUTH_TIKTOK', 'client_key', 0, 'TikTok ClientKey'),
('OAUTH_TIKTOK', 'client_secret', 1, 'TikTok Secret（加密）'),
('OAUTH_TIKTOK', 'redirect_uri', 0, '回调地址'),
('SECURITY', 'admin_api_key', 1, 'API认证密钥（加密）'),
('SECURITY', 'token_encrypt_key', 0, 'Token加密密钥（Base64）');
