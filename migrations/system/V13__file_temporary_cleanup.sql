-- 清理只操作登记的临时对象；保留失败及重试状态，不扫描或清空存储根目录。
ALTER TABLE sys_file_entry
    ADD COLUMN temporary_cleaned_at datetime NULL,
    ADD COLUMN cleanup_retry_at datetime NULL,
    ADD KEY idx_file_cleanup (temporary_cleaned_at, cleanup_retry_at, state, expires_at, id);

-- 普通直传此前未登记超时；崩溃遗留的 UPLOADING 也必须有明确期限。
UPDATE sys_file_entry SET expires_at=DATE_ADD(created_at, INTERVAL 1 HOUR)
WHERE state='UPLOADING' AND part_bytes IS NULL AND expires_at IS NULL;
