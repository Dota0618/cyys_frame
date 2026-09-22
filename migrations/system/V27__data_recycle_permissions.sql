-- 登记独立回收站权限；不自动给既有角色增加恢复或彻底删除权。
INSERT INTO sys_menu(id,code,name,parent_id,type,path,icon,sort,created_by)
SELECT 'nav-menu-data-recycle','nav:menu:data-recycle','数据回收站',
       COALESCE((SELECT id FROM sys_menu WHERE code='nav:group:system' AND deleted=0),'0'),
       2,'/system/recycle','DeleteOutlined',190,'migration-v27'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:data-recycle');

INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-recycle-read','recycle:read','查看数据回收站',
       (SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,1,'migration-v27'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='recycle:read');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-recycle-restore','recycle:restore','恢复数据',
       (SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,2,'migration-v27'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='recycle:restore');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-recycle-purge','recycle:purge','彻底删除数据',
       (SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,3,'migration-v27'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='recycle:purge');
