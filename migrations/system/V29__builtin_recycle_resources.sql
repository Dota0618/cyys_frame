-- 预置平台控制数据回收站资源；这些表不进入后台业务表选择下拉。
-- 通知和版本继续使用已有专用回收站资源，以保留阅读记录和附件关联处理。

INSERT INTO sys_recycle_resource(
    id,code,name,table_name,title_column,search_column,scope_required,scope_column,
    department_column,ancestors_column,actor_column,version_column,deleted_column,
    created_at_column,updated_at_column,updated_by_column,sort,enabled,created_by,updated_by)
SELECT 'recycle-resource-dict','system-dictionary','字典分类','param_basic','name','name',0,NULL,NULL,NULL,'created_by',
       'version','deleted','created_at','updated_at','updated_by',10,1,'migration-v29','migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_recycle_resource WHERE table_name='param_basic');

INSERT INTO sys_recycle_resource(
    id,code,name,table_name,title_column,search_column,scope_required,scope_column,
    department_column,ancestors_column,actor_column,version_column,deleted_column,
    created_at_column,updated_at_column,updated_by_column,sort,enabled,created_by,updated_by)
SELECT 'recycle-resource-dict-item','system-dictionary-item','字典数据','param_basic_type','param_value','param_value',0,NULL,NULL,NULL,'created_by',
       'version','deleted','created_at','updated_at','updated_by',20,1,'migration-v29','migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_recycle_resource WHERE table_name='param_basic_type');

INSERT INTO sys_recycle_resource(
    id,code,name,table_name,title_column,search_column,scope_required,scope_column,
    department_column,ancestors_column,actor_column,version_column,deleted_column,
    created_at_column,updated_at_column,updated_by_column,sort,enabled,created_by,updated_by)
SELECT 'recycle-resource-menu','system-menu','菜单数据','sys_menu','name','name',0,NULL,NULL,NULL,'created_by',
       'version','deleted','created_at','updated_at','updated_by',30,1,'migration-v29','migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_recycle_resource WHERE table_name='sys_menu');

INSERT INTO sys_recycle_resource(
    id,code,name,table_name,title_column,search_column,scope_required,scope_column,
    department_column,ancestors_column,actor_column,version_column,deleted_column,
    created_at_column,updated_at_column,updated_by_column,sort,enabled,created_by,updated_by)
SELECT 'recycle-resource-user','system-user','用户数据','sys_user','login_name','login_name',0,NULL,NULL,NULL,'created_by',
       'version','deleted','created_at','updated_at','updated_by',40,1,'migration-v29','migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_recycle_resource WHERE table_name='sys_user');

INSERT INTO sys_recycle_resource(
    id,code,name,table_name,title_column,search_column,scope_required,scope_column,
    department_column,ancestors_column,actor_column,version_column,deleted_column,
    created_at_column,updated_at_column,updated_by_column,sort,enabled,created_by,updated_by)
SELECT 'recycle-resource-role','system-role','角色数据','sys_role','name','name',0,NULL,NULL,NULL,'created_by',
       'version','deleted','created_at','updated_at','updated_by',50,1,'migration-v29','migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_recycle_resource WHERE table_name='sys_role');

INSERT INTO sys_recycle_resource(
    id,code,name,table_name,title_column,search_column,scope_required,scope_column,
    department_column,ancestors_column,actor_column,version_column,deleted_column,
    created_at_column,updated_at_column,updated_by_column,sort,enabled,created_by,updated_by)
SELECT 'recycle-resource-scope','system-scope','单位数据','sys_scope','name','name',0,NULL,NULL,NULL,'created_by',
       'version','deleted','created_at','updated_at','updated_by',60,1,'migration-v29','migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_recycle_resource WHERE table_name='sys_scope');

INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-dict-read','recycle:system-dictionary:read','查看字典分类',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,10,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-dictionary:read');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-dict-restore','recycle:system-dictionary:restore','恢复字典分类',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,11,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-dictionary:restore');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-dict-purge','recycle:system-dictionary:purge','彻底删除字典分类',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,12,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-dictionary:purge');

INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-dict-item-read','recycle:system-dictionary-item:read','查看字典数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,20,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-dictionary-item:read');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-dict-item-restore','recycle:system-dictionary-item:restore','恢复字典数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,21,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-dictionary-item:restore');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-dict-item-purge','recycle:system-dictionary-item:purge','彻底删除字典数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,22,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-dictionary-item:purge');

INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-menu-read','recycle:system-menu:read','查看菜单数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,30,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-menu:read');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-menu-restore','recycle:system-menu:restore','恢复菜单数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,31,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-menu:restore');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-menu-purge','recycle:system-menu:purge','彻底删除菜单数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,32,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-menu:purge');

INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-user-read','recycle:system-user:read','查看用户数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,40,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-user:read');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-user-restore','recycle:system-user:restore','恢复用户数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,41,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-user:restore');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-user-purge','recycle:system-user:purge','彻底删除用户数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,42,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-user:purge');

INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-role-read','recycle:system-role:read','查看角色数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,50,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-role:read');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-role-restore','recycle:system-role:restore','恢复角色数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,51,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-role:restore');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-role-purge','recycle:system-role:purge','彻底删除角色数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,52,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-role:purge');

INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-scope-read','recycle:system-scope:read','查看单位数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,60,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-scope:read');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-scope-restore','recycle:system-scope:restore','恢复单位数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,61,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-scope:restore');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-rb-scope-purge','recycle:system-scope:purge','彻底删除单位数据',(SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle'),3,62,'migration-v29'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE code='recycle:system-scope:purge');
