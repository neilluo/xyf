-- CR修复: 添加 account_id 索引 + platform_account 软删除改造

-- #6: distribution_task 添加 account_id 索引（覆盖幂等性查询）
CREATE INDEX idx_account ON distribution_task (account_id);

-- #7: platform_account 软删除改造（方案A: nullable deleted_at）
-- 利用 MySQL 对 NULL 值不参与唯一索引的特性：
-- 活跃记录 deleted_at=NULL（不冲突），已删除记录 deleted_at=具体时间戳（各不相同）
ALTER TABLE platform_account ADD COLUMN deleted_at DATETIME DEFAULT NULL COMMENT '删除时间，NULL表示未删除' AFTER ext_info;

-- 重建唯一索引：活跃记录通过 (platform, account_name, NULL) 保证唯一
-- 已删除记录因 deleted_at 各不相同，不会触发唯一约束
ALTER TABLE platform_account DROP INDEX uk_platform_account;
CREATE UNIQUE INDEX uk_platform_account ON platform_account (platform, account_name, deleted_at);
