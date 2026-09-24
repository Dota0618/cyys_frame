-- 扩展全局参数；已有参数值由管理员维护，不覆盖现有配置。
UPDATE sys_user
SET pwd_last_changed = COALESCE(pwd_last_changed, created_at, CURRENT_TIMESTAMP)
WHERE pwd_last_changed IS NULL;

INSERT INTO sys_configure(id, name, value, type, desp, sort)
SELECT 'global-copyright-text', 'COPYRIGHT_TEXT', '', 1,
       '页脚版权文本；为空时使用系统名称和当前年份。', 60
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'COPYRIGHT_TEXT');

INSERT INTO sys_configure(id, name, value, type, desp, sort)
SELECT 'global-maintenance-mode', 'MAINTENANCE_MODE', '0', 1,
       '维护模式；1 表示启用并限制普通账号登录，0 表示正常运行。', 70
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'MAINTENANCE_MODE');

INSERT INTO sys_configure(id, name, value, type, desp, sort)
SELECT 'global-password-expire-days', 'PASSWORD_EXPIRE_DAYS', '0', 1,
       '密码有效期，单位为天；0 表示永不过期。', 80
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'PASSWORD_EXPIRE_DAYS');

INSERT INTO sys_configure(id, name, value, type, desp, sort)
SELECT 'global-file-max-size-mb', 'FILE_MAX_SIZE_MB', '20', 1,
       '单个文件最大大小，单位为 MB。', 90
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'FILE_MAX_SIZE_MB');

INSERT INTO sys_configure(id, name, value, type, desp, sort)
SELECT 'global-export-max-rows', 'EXPORT_MAX_ROWS', '5000', 1,
       '单次同步导出最大行数。', 100
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'EXPORT_MAX_ROWS');

INSERT INTO sys_configure(id, name, value, type, desp, sort)
SELECT 'global-audit-retention-days', 'AUDIT_RETENTION_DAYS', '365', 1,
       '审计记录保留天数；当前仅保存参数，暂不执行后台自动清理。', 110
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'AUDIT_RETENTION_DAYS');

INSERT INTO sys_configure(id, name, value, type, desp, sort)
SELECT 'global-contact-phone', 'CONTACT_PHONE', '', 1,
       '系统联系联系电话；为空时不显示。', 120
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'CONTACT_PHONE');
