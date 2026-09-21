-- 一个管理体系共用角色目录；机构归属只决定数据范围，不再参与角色授权。
-- 如旧库存在不同机构的同编码角色，唯一约束明确拒绝迁移，由管理员先确认命名。
ALTER TABLE sys_role ADD CONSTRAINT uk_role_code UNIQUE (code);
ALTER TABLE sys_user_role
    DROP INDEX uk_user_role,
    ADD CONSTRAINT uk_user_role UNIQUE (user_id,role_id),
    DROP COLUMN scope_id;
ALTER TABLE sys_role DROP INDEX uk_role_scope_code, DROP INDEX uk_role_id_scope,
    DROP COLUMN scope_id;

-- 平台角色管理与原单位角色管理合并；已有权限映射到唯一的角色管理入口。
INSERT INTO sys_role_menu(id,role_id,menu_id,created_by)
SELECT REPLACE(UUID(),'-',''),old_grant.role_id,target.id,'migration-v23'
FROM sys_role_menu old_grant
JOIN sys_menu old_menu ON old_menu.id=old_grant.menu_id AND old_menu.deleted=0
JOIN sys_menu target ON target.code=REPLACE(old_menu.code,'platform:role:','role:') AND target.deleted=0
WHERE old_grant.deleted=0 AND old_menu.code LIKE 'platform:role:%'
  AND NOT EXISTS(SELECT 1 FROM sys_role_menu existing WHERE existing.role_id=old_grant.role_id AND existing.menu_id=target.id);

INSERT INTO sys_role_menu(id,role_id,menu_id,created_by)
SELECT REPLACE(UUID(),'-',''),old_grant.role_id,target.id,'migration-v23'
FROM sys_role_menu old_grant
JOIN sys_menu old_menu ON old_menu.id=old_grant.menu_id AND old_menu.path='/system/platform-roles' AND old_menu.deleted=0
JOIN sys_menu target ON target.path='/system/roles' AND target.deleted=0
WHERE old_grant.deleted=0
  AND NOT EXISTS(SELECT 1 FROM sys_role_menu existing WHERE existing.role_id=old_grant.role_id AND existing.menu_id=target.id);

UPDATE sys_menu SET status=0,visible=0,version=version+1
WHERE code LIKE 'platform:role:%' OR path='/system/platform-roles';
UPDATE sys_menu SET name='角色管理',version=version+1 WHERE path='/system/roles';
UPDATE sys_menu SET name='菜单管理',version=version+1 WHERE path='/system/platform-menus';
