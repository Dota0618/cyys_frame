-- 仅在显式选择 dev-data 时由同一个 Flyway 执行。不得打入生产迁移资源。
-- 缺少任一占位符时终止；密码散列由本地操作者提供，不内置已知密码。
INSERT INTO sys_scope (id, code, name, created_by)
VALUES ('${bootstrapScopeId}', '${bootstrapScopeId}', '本地开发单位', 'bootstrap');

INSERT INTO sys_user (id, login_name, password_hash, real_name, created_by)
VALUES ('${bootstrapAccountId}', '${bootstrapLoginName}', '${bootstrapPasswordHash}', '本地初始化账号', 'bootstrap');

INSERT INTO sys_user_scope (id, user_id, scope_id, member_role, default_flag, created_by)
VALUES ('local-membership', '${bootstrapAccountId}', '${bootstrapScopeId}', 'OWNER', 1, 'bootstrap');

INSERT INTO sys_role (id, scope_id, code, name, created_by)
VALUES ('local-platform-role', 'GLOBAL', 'LOCAL_PLATFORM_ACCESS', '本地平台单位访问', 'bootstrap');

INSERT INTO sys_user_role (id, user_id, role_id, scope_id, created_by)
VALUES ('local-platform-grant', '${bootstrapAccountId}', 'local-platform-role', 'GLOBAL', 'bootstrap');

INSERT INTO sys_menu (id, code, name, type, platform_only, created_by)
VALUES ('local-scope-access', 'platform:scope:access', '平台单位访问', 3, 1, 'bootstrap');

INSERT INTO sys_role_menu (id, role_id, menu_id, created_by)
VALUES ('local-scope-access-grant', 'local-platform-role', 'local-scope-access', 'bootstrap');
