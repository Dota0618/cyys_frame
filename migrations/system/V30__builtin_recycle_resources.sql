-- 将通知和版本登记为统一回收站资源，所有数据均走 sys_recycle_resource 动态配置。

INSERT INTO sys_recycle_resource(
    id,code,name,table_name,title_column,search_column,scope_required,scope_column,
    department_column,ancestors_column,actor_column,version_column,deleted_column,
    created_at_column,updated_at_column,updated_by_column,sort,enabled,created_by,updated_by)
SELECT 'recycle-resource-org-notice','org-notice','单位通知','sys_org_notice','title','title',1,'scope_id','org_id',NULL,'created_by',
       'version','deleted','created_at','updated_at','updated_by',70,1,'migration-v30','migration-v30'
WHERE NOT EXISTS (SELECT 1 FROM sys_recycle_resource WHERE table_name='sys_org_notice');

INSERT INTO sys_recycle_resource(
    id,code,name,table_name,title_column,search_column,scope_required,scope_column,
    department_column,ancestors_column,actor_column,version_column,deleted_column,
    created_at_column,updated_at_column,updated_by_column,sort,enabled,created_by,updated_by)
SELECT 'recycle-resource-system-notice','system-notice','系统通知','sys_notice','title','title',0,NULL,NULL,NULL,'created_by',
       'version','deleted','created_at','updated_at','updated_by',80,1,'migration-v30','migration-v30'
WHERE NOT EXISTS (SELECT 1 FROM sys_recycle_resource WHERE table_name='sys_notice');

INSERT INTO sys_recycle_resource(
    id,code,name,table_name,title_column,search_column,scope_required,scope_column,
    department_column,ancestors_column,actor_column,version_column,deleted_column,
    created_at_column,updated_at_column,updated_by_column,sort,enabled,created_by,updated_by)
SELECT 'recycle-resource-release','release','版本记录','sys_version','release_number','release_number',0,NULL,NULL,NULL,'created_by',
       'version','deleted','created_at','updated_at','updated_by',90,1,'migration-v30','migration-v30'
WHERE NOT EXISTS (SELECT 1 FROM sys_recycle_resource WHERE table_name='sys_version');

INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-org-notice-read','recycle:org-notice:read','查看单位通知',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,70,'migration-v30'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:org-notice:read');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-org-notice-restore','recycle:org-notice:restore','恢复单位通知',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,71,'migration-v30'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:org-notice:restore');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-org-notice-purge','recycle:org-notice:purge','彻底删除单位通知',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,72,'migration-v30'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:org-notice:purge');

INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-system-notice-read','recycle:system-notice:read','查看系统通知',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,80,'migration-v30'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-notice:read');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-system-notice-restore','recycle:system-notice:restore','恢复系统通知',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,81,'migration-v30'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-notice:restore');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-system-notice-purge','recycle:system-notice:purge','彻底删除系统通知',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,82,'migration-v30'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-notice:purge');

INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-release-read','recycle:release:read','查看版本记录',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,90,'migration-v30'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:release:read');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-release-restore','recycle:release:restore','恢复版本记录',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,91,'migration-v30'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:release:restore');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-release-purge','recycle:release:purge','彻底删除版本记录',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,92,'migration-v30'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:release:purge');
