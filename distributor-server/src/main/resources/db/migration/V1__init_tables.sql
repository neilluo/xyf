-- Video Distributor 初始表结构
-- 三张核心表: video_meta, distribution_task, platform_account

-- 视频元数据
CREATE TABLE IF NOT EXISTS video_meta (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL DEFAULT 1 COMMENT '用户ID（预留多用户扩展）',
    title VARCHAR(256) NOT NULL COMMENT '视频标题',
    description TEXT COMMENT '视频描述',
    tags JSON COMMENT '标签列表',
    category VARCHAR(64) COMMENT '分类',
    oss_bucket VARCHAR(64) NOT NULL,
    oss_key VARCHAR(512) NOT NULL COMMENT 'OSS对象Key',
    oss_region VARCHAR(32) NOT NULL DEFAULT 'ap-southeast-1',
    file_size BIGINT NOT NULL COMMENT '文件大小(bytes)',
    file_format VARCHAR(16) COMMENT '文件格式(mp4/mov等)',
    duration_seconds INT COMMENT '视频时长(秒)',
    resolution VARCHAR(16) COMMENT '分辨率(如1920x1080)',
    thumbnail_oss_key VARCHAR(512) COMMENT '缩略图OSS Key',
    ext_info JSON COMMENT '扩展参数，含traceId用于链路追溯',
    is_deleted TINYINT UNSIGNED DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频元数据';

-- 分发任务
CREATE TABLE IF NOT EXISTS distribution_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL DEFAULT 1 COMMENT '用户ID（预留）',
    video_id BIGINT NOT NULL COMMENT '关联video_meta.id',
    platform VARCHAR(32) NOT NULL COMMENT '目标平台: YOUTUBE|TIKTOK',
    account_id BIGINT NOT NULL COMMENT '关联platform_account.id',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|UPLOADING|PROCESSING|SUCCESS|FAILED',
    retry_count INT DEFAULT 0,
    max_retry INT DEFAULT 3,
    scheduled_at DATETIME NOT NULL COMMENT '计划执行时间',
    started_at DATETIME,
    completed_at DATETIME,
    error_message TEXT COMMENT '最近一次错误信息',
    platform_video_id VARCHAR(256) COMMENT '平台返回的视频ID/URL',
    upload_session_uri TEXT COMMENT '断点续传会话URI',
    upload_offset BIGINT DEFAULT 0 COMMENT '已上传字节数',
    platform_metadata JSON COMMENT '平台特有的元数据覆盖',
    ext_info JSON COMMENT '扩展参数，含traceId用于链路追溯',
    is_deleted TINYINT UNSIGNED DEFAULT 0 COMMENT '是否删除 0-否 1-是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status_schedule (status, scheduled_at),
    INDEX idx_video (video_id),
    INDEX idx_platform_status (platform, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分发任务';

-- 平台账号配置
CREATE TABLE IF NOT EXISTS platform_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL DEFAULT 1 COMMENT '用户ID（预留）',
    platform VARCHAR(32) NOT NULL COMMENT 'YOUTUBE|TIKTOK',
    account_name VARCHAR(128) NOT NULL COMMENT '账号标识/名称',
    access_token TEXT COMMENT '访问令牌(加密存储)',
    refresh_token TEXT COMMENT '刷新令牌(加密存储)',
    token_expires_at DATETIME COMMENT 'access_token过期时间',
    channel_id VARCHAR(128) COMMENT 'YouTube频道ID / TikTok open_id',
    extra_config JSON COMMENT '平台特有配置',
    ext_info JSON COMMENT '扩展参数，含traceId用于链路追溯',
    status VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE|EXPIRED|REVOKED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_platform_account (platform, account_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台账号配置';
