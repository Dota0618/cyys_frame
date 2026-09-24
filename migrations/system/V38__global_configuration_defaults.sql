-- 全局参数为系统内置记录：可修改，禁止删除。
INSERT INTO sys_configure (id, name, `value`, type, desp, sort)
SELECT 'global-system-name', 'SYSTEM_NAME', 'CYYS', 1, '系统名称，显示在登录页、主页面左上角和浏览器标签页等位置。', 10
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'SYSTEM_NAME');

INSERT INTO sys_configure (id, name, `value`, type, desp, sort)
SELECT 'global-system-province', 'SYSTEM_PROVINCE_CODE', NULL, 1, '系统地区编码；设置后地区管理树和地区选择仅显示该地区及其下级。', 20
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'SYSTEM_PROVINCE_CODE');

INSERT INTO sys_configure (id, name, `value`, type, desp, sort)
SELECT 'global-password-strength', 'PWD_STRENGTH', '0', 1,
       'System user password strength: 0 = no requirement; 1 = requires letters and numbers; 2 = requires uppercase, lowercase, and numbers. All passwords must be at least 8 characters long.', 30
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'PWD_STRENGTH');

INSERT INTO sys_configure (id, name, `value`, type, desp, sort)
SELECT 'global-password-lock-num', 'PWD_LOCK_NUM', '5', 1, '连续密码错误达到此次数后锁定；设置为 0 表示不锁定。', 40
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'PWD_LOCK_NUM');

INSERT INTO sys_configure (id, name, `value`, type, desp, sort)
SELECT 'global-password-lock-hour', 'PWD_LOCK_HOUR', '1', 1, '密码锁定持续时间，单位为小时。', 50
WHERE NOT EXISTS (SELECT 1 FROM sys_configure WHERE name = 'PWD_LOCK_HOUR');
