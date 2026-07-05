-- CR修复: 添加 account_id 索引 + platform_account 添加 is_deleted 字段

-- #6: distribution_task 添加 account_id 索引（覆盖幂等性查询）
CREATE INDEX idx_account ON distribution_task (account_id);

-- #7: platform_account 添加 is_deleted 软删除字段
ALTER TABLE platform_account ADD COLUMN is_deleted TINYINT UNSIGNED DEFAULT 0 COMMENT '是否删除 0-否 1-是' AFTER ext_info;

-- 重建唯一索引（包含 is_deleted，允许软删除后重新创建同名账号）
ALTER TABLE platform_account DROP INDEX uk_platform_account;
CREATE UNIQUE INDEX uk_platform_account ON platform_account (platform, account_name, is_deleted);
