-- 中文菜单目录初始化/升级数据：仅含数据操作，不创建账号、单位或改变表结构。
-- 在正式迁移后显式执行；先备份，整个文件在同一个数据库事务内执行。
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-group-organization','nav:group:organization','组织管理','0',1,NULL,10,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:group:organization');
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-group-system','nav:group:system','系统管理','0',1,NULL,20,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:group:system');
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-group-files','nav:group:files','文件管理','0',1,NULL,30,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:group:files');
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-group-content','nav:group:content','通知与版本','0',1,NULL,40,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:group:content');
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-group-development','nav:group:development','开发工具','0',1,NULL,50,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:group:development');
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-scopes','nav:menu:scopes','单位管理',(SELECT id FROM sys_menu WHERE code='nav:group:organization'),2,'/system/scopes',10,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:scopes');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:organization' SET m.name='单位管理',m.parent_id=p.id,m.sort=10,m.version=m.version+1 WHERE m.code='nav:menu:scopes';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-1','platform:scope:read','查看单位',(SELECT id FROM sys_menu WHERE code='nav:menu:scopes'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='platform:scope:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:scopes' SET m.name='查看单位',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='platform:scope:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-2','platform:scope:create','新增单位',(SELECT id FROM sys_menu WHERE code='nav:menu:scopes'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='platform:scope:create');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:scopes' SET m.name='新增单位',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='platform:scope:create';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-3','platform:scope:update','修改及启停单位',(SELECT id FROM sys_menu WHERE code='nav:menu:scopes'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='platform:scope:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:scopes' SET m.name='修改及启停单位',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='platform:scope:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-4','platform:scope:access','跨单位访问',(SELECT id FROM sys_menu WHERE code='nav:menu:scopes'),3,NULL,4,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='platform:scope:access');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:scopes' SET m.name='跨单位访问',m.parent_id=p.id,m.sort=4,m.version=m.version+1 WHERE m.code='platform:scope:access';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-5','platform:data:read-all','跨单位只读',(SELECT id FROM sys_menu WHERE code='nav:menu:scopes'),3,NULL,5,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='platform:data:read-all');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:scopes' SET m.name='跨单位只读',m.parent_id=p.id,m.sort=5,m.version=m.version+1 WHERE m.code='platform:data:read-all';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-6','platform:data:operate','跨单位操作',(SELECT id FROM sys_menu WHERE code='nav:menu:scopes'),3,NULL,6,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='platform:data:operate');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:scopes' SET m.name='跨单位操作',m.parent_id=p.id,m.sort=6,m.version=m.version+1 WHERE m.code='platform:data:operate';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-users','nav:menu:users','用户管理',(SELECT id FROM sys_menu WHERE code='nav:group:organization'),2,'/system/users',20,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:users');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:organization' SET m.name='用户管理',m.parent_id=p.id,m.sort=20,m.version=m.version+1 WHERE m.code='nav:menu:users';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-7','user:read','查看用户',(SELECT id FROM sys_menu WHERE code='nav:menu:users'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='user:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:users' SET m.name='查看用户',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='user:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-8','user:create','新增用户',(SELECT id FROM sys_menu WHERE code='nav:menu:users'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='user:create');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:users' SET m.name='新增用户',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='user:create';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-9','user:update','修改用户',(SELECT id FROM sys_menu WHERE code='nav:menu:users'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='user:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:users' SET m.name='修改用户',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='user:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-10','user:export','导出用户',(SELECT id FROM sys_menu WHERE code='nav:menu:users'),3,NULL,4,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='user:export');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:users' SET m.name='导出用户',m.parent_id=p.id,m.sort=4,m.version=m.version+1 WHERE m.code='user:export';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-11','user:display:read','查看用户名称',(SELECT id FROM sys_menu WHERE code='nav:menu:users'),3,NULL,5,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='user:display:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:users' SET m.name='查看用户名称',m.parent_id=p.id,m.sort=5,m.version=m.version+1 WHERE m.code='user:display:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-12','platform:account:reset-password','重置账号密码',(SELECT id FROM sys_menu WHERE code='nav:menu:users'),3,NULL,6,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='platform:account:reset-password');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:users' SET m.name='重置账号密码',m.parent_id=p.id,m.sort=6,m.version=m.version+1 WHERE m.code='platform:account:reset-password';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-departments','nav:menu:departments','部门管理',(SELECT id FROM sys_menu WHERE code='nav:group:organization'),2,'/system/departments',30,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:departments');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:organization' SET m.name='部门管理',m.parent_id=p.id,m.sort=30,m.version=m.version+1 WHERE m.code='nav:menu:departments';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-13','department:read','查看部门',(SELECT id FROM sys_menu WHERE code='nav:menu:departments'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='department:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:departments' SET m.name='查看部门',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='department:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-14','department:create','新增部门',(SELECT id FROM sys_menu WHERE code='nav:menu:departments'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='department:create');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:departments' SET m.name='新增部门',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='department:create';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-15','department:update','修改部门',(SELECT id FROM sys_menu WHERE code='nav:menu:departments'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='department:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:departments' SET m.name='修改部门',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='department:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-16','department:delete','删除部门',(SELECT id FROM sys_menu WHERE code='nav:menu:departments'),3,NULL,4,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='department:delete');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:departments' SET m.name='删除部门',m.parent_id=p.id,m.sort=4,m.version=m.version+1 WHERE m.code='department:delete';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-roles','nav:menu:roles','单位角色',(SELECT id FROM sys_menu WHERE code='nav:group:organization'),2,'/system/roles',40,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:roles');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:organization' SET m.name='单位角色',m.parent_id=p.id,m.sort=40,m.version=m.version+1 WHERE m.code='nav:menu:roles';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-17','role:read','查看角色',(SELECT id FROM sys_menu WHERE code='nav:menu:roles'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='role:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:roles' SET m.name='查看角色',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='role:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-18','role:create','新增角色',(SELECT id FROM sys_menu WHERE code='nav:menu:roles'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='role:create');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:roles' SET m.name='新增角色',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='role:create';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-19','role:update','修改角色',(SELECT id FROM sys_menu WHERE code='nav:menu:roles'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='role:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:roles' SET m.name='修改角色',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='role:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-20','role:delete','删除角色',(SELECT id FROM sys_menu WHERE code='nav:menu:roles'),3,NULL,4,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='role:delete');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:roles' SET m.name='删除角色',m.parent_id=p.id,m.sort=4,m.version=m.version+1 WHERE m.code='role:delete';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-21','role:grant','授予角色',(SELECT id FROM sys_menu WHERE code='nav:menu:roles'),3,NULL,5,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='role:grant');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:roles' SET m.name='授予角色',m.parent_id=p.id,m.sort=5,m.version=m.version+1 WHERE m.code='role:grant';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-platform-menus','nav:menu:platform-menus','菜单管理',(SELECT id FROM sys_menu WHERE code='nav:group:system'),2,'/system/platform-menus',50,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:platform-menus');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:system' SET m.name='菜单管理',m.parent_id=p.id,m.sort=50,m.version=m.version+1 WHERE m.code='nav:menu:platform-menus';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-22','menu:read','查看菜单',(SELECT id FROM sys_menu WHERE code='nav:menu:platform-menus'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='menu:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:platform-menus' SET m.name='查看菜单',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='menu:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-23','menu:create','新增菜单',(SELECT id FROM sys_menu WHERE code='nav:menu:platform-menus'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='menu:create');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:platform-menus' SET m.name='新增菜单',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='menu:create';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-24','menu:update','修改菜单',(SELECT id FROM sys_menu WHERE code='nav:menu:platform-menus'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='menu:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:platform-menus' SET m.name='修改菜单',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='menu:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-25','menu:delete','删除菜单',(SELECT id FROM sys_menu WHERE code='nav:menu:platform-menus'),3,NULL,4,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='menu:delete');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:platform-menus' SET m.name='删除菜单',m.parent_id=p.id,m.sort=4,m.version=m.version+1 WHERE m.code='menu:delete';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-platform-roles','nav:menu:platform-roles','平台角色',(SELECT id FROM sys_menu WHERE code='nav:group:system'),2,'/system/platform-roles',60,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:platform-roles');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:system' SET m.name='平台角色',m.parent_id=p.id,m.sort=60,m.version=m.version+1 WHERE m.code='nav:menu:platform-roles';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-26','platform:role:read','查看平台角色',(SELECT id FROM sys_menu WHERE code='nav:menu:platform-roles'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='platform:role:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:platform-roles' SET m.name='查看平台角色',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='platform:role:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-27','platform:role:create','新增平台角色',(SELECT id FROM sys_menu WHERE code='nav:menu:platform-roles'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='platform:role:create');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:platform-roles' SET m.name='新增平台角色',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='platform:role:create';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-28','platform:role:update','修改平台角色',(SELECT id FROM sys_menu WHERE code='nav:menu:platform-roles'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='platform:role:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:platform-roles' SET m.name='修改平台角色',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='platform:role:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-29','platform:role:delete','删除平台角色',(SELECT id FROM sys_menu WHERE code='nav:menu:platform-roles'),3,NULL,4,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='platform:role:delete');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:platform-roles' SET m.name='删除平台角色',m.parent_id=p.id,m.sort=4,m.version=m.version+1 WHERE m.code='platform:role:delete';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-30','platform:role:grant','授予平台角色',(SELECT id FROM sys_menu WHERE code='nav:menu:platform-roles'),3,NULL,5,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='platform:role:grant');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:platform-roles' SET m.name='授予平台角色',m.parent_id=p.id,m.sort=5,m.version=m.version+1 WHERE m.code='platform:role:grant';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-configurations','nav:menu:configurations','通用配置',(SELECT id FROM sys_menu WHERE code='nav:group:system'),2,'/system/configurations',70,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:configurations');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:system' SET m.name='通用配置',m.parent_id=p.id,m.sort=70,m.version=m.version+1 WHERE m.code='nav:menu:configurations';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-31','configuration:read','查看配置',(SELECT id FROM sys_menu WHERE code='nav:menu:configurations'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='configuration:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:configurations' SET m.name='查看配置',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='configuration:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-32','configuration:create','新增配置',(SELECT id FROM sys_menu WHERE code='nav:menu:configurations'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='configuration:create');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:configurations' SET m.name='新增配置',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='configuration:create';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-33','configuration:update','修改配置',(SELECT id FROM sys_menu WHERE code='nav:menu:configurations'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='configuration:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:configurations' SET m.name='修改配置',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='configuration:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-34','configuration:delete','删除配置',(SELECT id FROM sys_menu WHERE code='nav:menu:configurations'),3,NULL,4,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='configuration:delete');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:configurations' SET m.name='删除配置',m.parent_id=p.id,m.sort=4,m.version=m.version+1 WHERE m.code='configuration:delete';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-dictionaries','nav:menu:dictionaries','数据字典',(SELECT id FROM sys_menu WHERE code='nav:group:system'),2,'/system/dictionaries',80,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:dictionaries');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:system' SET m.name='数据字典',m.parent_id=p.id,m.sort=80,m.version=m.version+1 WHERE m.code='nav:menu:dictionaries';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-35','dictionary:read','查看字典',(SELECT id FROM sys_menu WHERE code='nav:menu:dictionaries'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='dictionary:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:dictionaries' SET m.name='查看字典',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='dictionary:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-36','dictionary:create','新增字典',(SELECT id FROM sys_menu WHERE code='nav:menu:dictionaries'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='dictionary:create');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:dictionaries' SET m.name='新增字典',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='dictionary:create';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-37','dictionary:update','修改字典',(SELECT id FROM sys_menu WHERE code='nav:menu:dictionaries'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='dictionary:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:dictionaries' SET m.name='修改字典',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='dictionary:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-38','dictionary:delete','删除字典',(SELECT id FROM sys_menu WHERE code='nav:menu:dictionaries'),3,NULL,4,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='dictionary:delete');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:dictionaries' SET m.name='删除字典',m.parent_id=p.id,m.sort=4,m.version=m.version+1 WHERE m.code='dictionary:delete';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-regions','nav:menu:regions','地区管理',(SELECT id FROM sys_menu WHERE code='nav:group:system'),2,'/system/regions',90,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:regions');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:system' SET m.name='地区管理',m.parent_id=p.id,m.sort=90,m.version=m.version+1 WHERE m.code='nav:menu:regions';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-39','region:read','查看地区',(SELECT id FROM sys_menu WHERE code='nav:menu:regions'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='region:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:regions' SET m.name='查看地区',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='region:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-40','region:create','新增地区',(SELECT id FROM sys_menu WHERE code='nav:menu:regions'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='region:create');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:regions' SET m.name='新增地区',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='region:create';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-41','region:update','修改地区',(SELECT id FROM sys_menu WHERE code='nav:menu:regions'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='region:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:regions' SET m.name='修改地区',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='region:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-42','region:delete','删除地区',(SELECT id FROM sys_menu WHERE code='nav:menu:regions'),3,NULL,4,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='region:delete');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:regions' SET m.name='删除地区',m.parent_id=p.id,m.sort=4,m.version=m.version+1 WHERE m.code='region:delete';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-43','region:export','导出地区',(SELECT id FROM sys_menu WHERE code='nav:menu:regions'),3,NULL,5,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='region:export');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:regions' SET m.name='导出地区',m.parent_id=p.id,m.sort=5,m.version=m.version+1 WHERE m.code='region:export';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-audits','nav:menu:audits','操作审计',(SELECT id FROM sys_menu WHERE code='nav:group:system'),2,'/system/audits',100,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:audits');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:system' SET m.name='操作审计',m.parent_id=p.id,m.sort=100,m.version=m.version+1 WHERE m.code='nav:menu:audits';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-44','audit:read','查看操作记录',(SELECT id FROM sys_menu WHERE code='nav:menu:audits'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='audit:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:audits' SET m.name='查看操作记录',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='audit:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-login-audits','nav:menu:login-audits','登录记录',(SELECT id FROM sys_menu WHERE code='nav:group:system'),2,'/system/login-audits',110,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:login-audits');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:system' SET m.name='登录记录',m.parent_id=p.id,m.sort=110,m.version=m.version+1 WHERE m.code='nav:menu:login-audits';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-45','login-audit:read','查看登录记录',(SELECT id FROM sys_menu WHERE code='nav:menu:login-audits'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='login-audit:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:login-audits' SET m.name='查看登录记录',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='login-audit:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-caches','nav:menu:caches','缓存管理',(SELECT id FROM sys_menu WHERE code='nav:group:system'),2,'/system/caches',120,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:caches');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:system' SET m.name='缓存管理',m.parent_id=p.id,m.sort=120,m.version=m.version+1 WHERE m.code='nav:menu:caches';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-46','cache:user:refresh','刷新单位缓存',(SELECT id FROM sys_menu WHERE code='nav:menu:caches'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='cache:user:refresh');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:caches' SET m.name='刷新单位缓存',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='cache:user:refresh';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-files','nav:menu:files','文件管理',(SELECT id FROM sys_menu WHERE code='nav:group:files'),2,'/system/files',130,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:files');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:files' SET m.name='文件管理',m.parent_id=p.id,m.sort=130,m.version=m.version+1 WHERE m.code='nav:menu:files';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-47','file:read','查看文件',(SELECT id FROM sys_menu WHERE code='nav:menu:files'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='file:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:files' SET m.name='查看文件',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='file:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-48','file:content:read','下载文件',(SELECT id FROM sys_menu WHERE code='nav:menu:files'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='file:content:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:files' SET m.name='下载文件',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='file:content:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-49','file:upload','上传文件',(SELECT id FROM sys_menu WHERE code='nav:menu:files'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='file:upload');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:files' SET m.name='上传文件',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='file:upload';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-50','file:update','修改文件',(SELECT id FROM sys_menu WHERE code='nav:menu:files'),3,NULL,4,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='file:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:files' SET m.name='修改文件',m.parent_id=p.id,m.sort=4,m.version=m.version+1 WHERE m.code='file:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-51','file:delete','删除文件',(SELECT id FROM sys_menu WHERE code='nav:menu:files'),3,NULL,5,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='file:delete');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:files' SET m.name='删除文件',m.parent_id=p.id,m.sort=5,m.version=m.version+1 WHERE m.code='file:delete';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-52','file:move','移动文件',(SELECT id FROM sys_menu WHERE code='nav:menu:files'),3,NULL,6,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='file:move');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:files' SET m.name='移动文件',m.parent_id=p.id,m.sort=6,m.version=m.version+1 WHERE m.code='file:move';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-53','file:directory:create','新增目录',(SELECT id FROM sys_menu WHERE code='nav:menu:files'),3,NULL,7,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='file:directory:create');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:files' SET m.name='新增目录',m.parent_id=p.id,m.sort=7,m.version=m.version+1 WHERE m.code='file:directory:create';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-54','file:directory:update','修改目录',(SELECT id FROM sys_menu WHERE code='nav:menu:files'),3,NULL,8,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='file:directory:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:files' SET m.name='修改目录',m.parent_id=p.id,m.sort=8,m.version=m.version+1 WHERE m.code='file:directory:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-55','file:directory:delete','删除目录',(SELECT id FROM sys_menu WHERE code='nav:menu:files'),3,NULL,9,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='file:directory:delete');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:files' SET m.name='删除目录',m.parent_id=p.id,m.sort=9,m.version=m.version+1 WHERE m.code='file:directory:delete';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-file-recycle','nav:menu:file-recycle','文件回收站',(SELECT id FROM sys_menu WHERE code='nav:group:files'),2,'/system/file-recycle',140,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:file-recycle');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:files' SET m.name='文件回收站',m.parent_id=p.id,m.sort=140,m.version=m.version+1 WHERE m.code='nav:menu:file-recycle';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-56','file:recycle:read','查看回收站',(SELECT id FROM sys_menu WHERE code='nav:menu:file-recycle'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='file:recycle:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:file-recycle' SET m.name='查看回收站',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='file:recycle:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-57','file:restore','恢复文件',(SELECT id FROM sys_menu WHERE code='nav:menu:file-recycle'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='file:restore');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:file-recycle' SET m.name='恢复文件',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='file:restore';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-58','file:purge','彻底删除文件',(SELECT id FROM sys_menu WHERE code='nav:menu:file-recycle'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='file:purge');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:file-recycle' SET m.name='彻底删除文件',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='file:purge';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-org-notices','nav:menu:org-notices','单位通知',(SELECT id FROM sys_menu WHERE code='nav:group:content'),2,'/system/org-notices',150,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:org-notices');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:content' SET m.name='单位通知',m.parent_id=p.id,m.sort=150,m.version=m.version+1 WHERE m.code='nav:menu:org-notices';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-59','org-notice:read','查看单位通知',(SELECT id FROM sys_menu WHERE code='nav:menu:org-notices'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='org-notice:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:org-notices' SET m.name='查看单位通知',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='org-notice:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-60','org-notice:create','新增单位通知',(SELECT id FROM sys_menu WHERE code='nav:menu:org-notices'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='org-notice:create');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:org-notices' SET m.name='新增单位通知',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='org-notice:create';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-61','org-notice:update','修改单位通知',(SELECT id FROM sys_menu WHERE code='nav:menu:org-notices'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='org-notice:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:org-notices' SET m.name='修改单位通知',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='org-notice:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-62','org-notice:delete','删除单位通知',(SELECT id FROM sys_menu WHERE code='nav:menu:org-notices'),3,NULL,4,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='org-notice:delete');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:org-notices' SET m.name='删除单位通知',m.parent_id=p.id,m.sort=4,m.version=m.version+1 WHERE m.code='org-notice:delete';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-63','org-notice:receipts:read','查看阅读记录',(SELECT id FROM sys_menu WHERE code='nav:menu:org-notices'),3,NULL,5,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='org-notice:receipts:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:org-notices' SET m.name='查看阅读记录',m.parent_id=p.id,m.sort=5,m.version=m.version+1 WHERE m.code='org-notice:receipts:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-system-notices','nav:menu:system-notices','系统通知',(SELECT id FROM sys_menu WHERE code='nav:group:content'),2,'/system/system-notices',160,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:system-notices');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:content' SET m.name='系统通知',m.parent_id=p.id,m.sort=160,m.version=m.version+1 WHERE m.code='nav:menu:system-notices';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-64','system-notice:read','查看系统通知',(SELECT id FROM sys_menu WHERE code='nav:menu:system-notices'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='system-notice:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:system-notices' SET m.name='查看系统通知',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='system-notice:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-65','system-notice:create','新增系统通知',(SELECT id FROM sys_menu WHERE code='nav:menu:system-notices'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='system-notice:create');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:system-notices' SET m.name='新增系统通知',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='system-notice:create';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-66','system-notice:update','修改系统通知',(SELECT id FROM sys_menu WHERE code='nav:menu:system-notices'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='system-notice:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:system-notices' SET m.name='修改系统通知',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='system-notice:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-67','system-notice:delete','删除系统通知',(SELECT id FROM sys_menu WHERE code='nav:menu:system-notices'),3,NULL,4,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='system-notice:delete');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:system-notices' SET m.name='删除系统通知',m.parent_id=p.id,m.sort=4,m.version=m.version+1 WHERE m.code='system-notice:delete';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-releases','nav:menu:releases','版本管理',(SELECT id FROM sys_menu WHERE code='nav:group:content'),2,'/system/releases',170,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:releases');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:content' SET m.name='版本管理',m.parent_id=p.id,m.sort=170,m.version=m.version+1 WHERE m.code='nav:menu:releases';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-68','release:read','查看版本',(SELECT id FROM sys_menu WHERE code='nav:menu:releases'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='release:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:releases' SET m.name='查看版本',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='release:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-69','release:create','新增版本',(SELECT id FROM sys_menu WHERE code='nav:menu:releases'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='release:create');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:releases' SET m.name='新增版本',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='release:create';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-70','release:update','修改版本',(SELECT id FROM sys_menu WHERE code='nav:menu:releases'),3,NULL,3,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='release:update');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:releases' SET m.name='修改版本',m.parent_id=p.id,m.sort=3,m.version=m.version+1 WHERE m.code='release:update';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-71','release:delete','删除版本',(SELECT id FROM sys_menu WHERE code='nav:menu:releases'),3,NULL,4,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='release:delete');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:releases' SET m.name='删除版本',m.parent_id=p.id,m.sort=4,m.version=m.version+1 WHERE m.code='release:delete';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-menu-generator','nav:menu:generator','代码生成器',(SELECT id FROM sys_menu WHERE code='nav:group:development'),2,'/system/generator',180,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='nav:menu:generator');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:group:development' SET m.name='代码生成器',m.parent_id=p.id,m.sort=180,m.version=m.version+1 WHERE m.code='nav:menu:generator';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-72','generator:read','预览代码',(SELECT id FROM sys_menu WHERE code='nav:menu:generator'),3,NULL,1,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='generator:read');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:generator' SET m.name='预览代码',m.parent_id=p.id,m.sort=1,m.version=m.version+1 WHERE m.code='generator:read';
INSERT INTO sys_menu(id,code,name,parent_id,type,path,sort,created_by)
SELECT 'nav-action-73','generator:generate','下载代码包',(SELECT id FROM sys_menu WHERE code='nav:menu:generator'),3,NULL,2,'system-migration'
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE code='generator:generate');
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:generator' SET m.name='下载代码包',m.parent_id=p.id,m.sort=2,m.version=m.version+1 WHERE m.code='generator:generate';

-- 已有超级管理员继续获得新增的单位管理权限，不按登录名设置绕过。
-- V21 用户数据权限归入用户管理；空库先执行 V21 再初始化菜单时同样挂接。
UPDATE sys_menu m JOIN sys_menu p ON p.code='nav:menu:users'
SET m.parent_id=p.id,m.name='配置数据权限',m.sort=7,m.version=m.version+1
WHERE m.code='user:data-permission';
INSERT INTO sys_role_menu(id,role_id,menu_id,created_by)
SELECT REPLACE(UUID(),'-',''),r.id,m.id,'system-migration'
FROM sys_role r JOIN sys_menu m ON m.code IN ('platform:scope:read','platform:scope:create','platform:scope:update')
WHERE r.code='SUPER_ADMIN' AND r.status=1 AND r.deleted=0
AND NOT EXISTS(SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.id AND rm.menu_id=m.id);

-- 迁移已有按钮授权的菜单与目录；以后由角色树复选框显式维护。
INSERT INTO sys_role_menu(id,role_id,menu_id,created_by)
SELECT REPLACE(UUID(),'-',''),g.role_id,g.menu_id,'system-migration'
FROM (SELECT DISTINCT rm.role_id,p.id AS menu_id FROM sys_role_menu rm
JOIN sys_menu b ON b.id=rm.menu_id AND b.type=3 AND b.deleted=0
JOIN sys_menu p ON p.id=b.parent_id AND p.type=2 AND p.deleted=0
WHERE rm.deleted=0) g
WHERE NOT EXISTS(SELECT 1 FROM sys_role_menu e WHERE e.role_id=g.role_id AND e.menu_id=g.menu_id);

INSERT INTO sys_role_menu(id,role_id,menu_id,created_by)
SELECT REPLACE(UUID(),'-',''),g.role_id,g.menu_id,'system-migration'
FROM (SELECT DISTINCT rm.role_id,p.id AS menu_id FROM sys_role_menu rm
JOIN sys_menu m ON m.id=rm.menu_id AND m.type=2 AND m.deleted=0
JOIN sys_menu p ON p.id=m.parent_id AND p.type=1 AND p.deleted=0
WHERE rm.deleted=0) g
WHERE NOT EXISTS(SELECT 1 FROM sys_role_menu e WHERE e.role_id=g.role_id AND e.menu_id=g.menu_id);

-- 当前模型只开放一个角色目录；代码生成器按本轮范围暂停。
UPDATE sys_menu SET status=0,visible=0,version=version+1 WHERE code LIKE 'platform:role:%' OR path='/system/platform-roles' OR code IN ('nav:menu:generator','generator:read','generator:generate');
UPDATE sys_menu SET name='机构管理',version=version+1 WHERE code='nav:menu:scopes';
UPDATE sys_menu SET name='菜单管理',version=version+1 WHERE path='/system/platform-menus';

UPDATE sys_menu SET deleted=1,status=0,visible=0,version=version+1
WHERE code IN ('platform:scope:access','platform:data:operate','platform:data:read-all')
   OR code LIKE 'platform:role:%' OR path='/system/platform-roles';
