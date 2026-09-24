-- 回收站资源登记表：只保存已核验的业务表和字段映射，不保存或执行任意 SQL。
CREATE TABLE sys_recycle_resource (
    id varchar(32) NOT NULL PRIMARY KEY,
    code varchar(50) NOT NULL,
    name varchar(50) NOT NULL,
    table_name varchar(64) NOT NULL,
    title_column varchar(64) NOT NULL,
    search_column varchar(64) NOT NULL,
    scope_required tinyint NOT NULL DEFAULT 0,
    scope_column varchar(64) NULL,
    department_column varchar(64) NULL,
    ancestors_column varchar(64) NULL,
    actor_column varchar(64) NULL,
    version_column varchar(64) NOT NULL,
    deleted_column varchar(64) NOT NULL,
    created_at_column varchar(64) NOT NULL,
    updated_at_column varchar(64) NOT NULL,
    updated_by_column varchar(64) NOT NULL,
    sort int NOT NULL DEFAULT 0,
    version int NOT NULL DEFAULT 0,
    enabled tinyint NOT NULL DEFAULT 1,
    deleted tinyint NOT NULL DEFAULT 0,
    created_by varchar(32) NOT NULL,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32) NOT NULL,
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_recycle_resource_code UNIQUE (code),
    CONSTRAINT uk_recycle_resource_table UNIQUE (table_name),
    CONSTRAINT ck_recycle_resource_scope CHECK (scope_required IN (0,1)),
    CONSTRAINT ck_recycle_resource_enabled CHECK (enabled IN (0,1)),
    CONSTRAINT ck_recycle_resource_deleted CHECK (deleted IN (0,1)),
    CONSTRAINT ck_recycle_resource_version CHECK (version >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sys_menu(id,code,name,parent_id,type,path,icon,sort,created_by)
SELECT 'nav-menu-recycle-config','nav:menu:recycle-config','回收站配置',
       (SELECT id FROM sys_menu WHERE code='nav:menu:data-recycle' AND deleted=0),2,'/system/recycle-config','SettingOutlined',191,'migration-v28'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:recycle-config');

INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-recycle-config-read','recycle:config:read','查看回收站配置',(SELECT id FROM sys_menu WHERE code='nav:menu:recycle-config'),3,1,'migration-v28'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='recycle:config:read');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-recycle-config-create','recycle:config:create','登记回收站资源',(SELECT id FROM sys_menu WHERE code='nav:menu:recycle-config'),3,2,'migration-v28'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='recycle:config:create');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-recycle-config-update','recycle:config:update','修改回收站配置',(SELECT id FROM sys_menu WHERE code='nav:menu:recycle-config'),3,3,'migration-v28'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='recycle:config:update');
INSERT INTO sys_menu(id,code,name,parent_id,type,sort,created_by)
SELECT 'perm-recycle-config-delete','recycle:config:delete','删除回收站配置',(SELECT id FROM sys_menu WHERE code='nav:menu:recycle-config'),3,4,'migration-v28'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='recycle:config:delete');
