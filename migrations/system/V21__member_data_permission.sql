-- 用户确认不兼容/迁移旧角色数据范围；每个单位成员独立配置，全部初始化为本人。
ALTER TABLE sys_user_scope ADD COLUMN data_range TINYINT NOT NULL DEFAULT 5,
    ADD CONSTRAINT ck_member_data_range CHECK (data_range BETWEEN 1 AND 5);

-- 关联键跟随成员表的实际排序规则，避免旧库字符串关联不一致。
SET @member_collation = (SELECT TABLE_COLLATION FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user_scope');
SET @member_charset = (SELECT CHARACTER_SET_NAME FROM information_schema.COLLATION_CHARACTER_SET_APPLICABILITY
    WHERE COLLATION_NAME = @member_collation);
SET @member_org_ddl = CONCAT('CREATE TABLE sys_user_scope_org (',
    'member_id varchar(32) NOT NULL, scope_id varchar(32) NOT NULL, org_id varchar(32) NOT NULL,',
    'PRIMARY KEY(member_id,org_id), INDEX idx_member_data_org(scope_id,org_id)',
    ') ENGINE=InnoDB DEFAULT CHARSET=', @member_charset, ' COLLATE=', @member_collation);
PREPARE member_org_statement FROM @member_org_ddl;
EXECUTE member_org_statement;
DEALLOCATE PREPARE member_org_statement;

DROP TABLE sys_role_org;
ALTER TABLE sys_role DROP CHECK ck_role_data_range, DROP COLUMN data_range;

INSERT INTO sys_menu(id,code,name,parent_id,type,platform_only,sort,created_by)
VALUES('perm-user-data-permission','user:data-permission','配置数据权限',
       COALESCE((SELECT p.id FROM (SELECT id FROM sys_menu WHERE code='nav:menu:users' AND deleted=0) p),'0'),
       3,0,7,'migration-v21');

-- 原角色授予者此前已经可以通过角色配置数据范围，继续授予独立配置操作权。
INSERT INTO sys_role_menu(id,role_id,menu_id,created_by)
SELECT REPLACE(UUID(),'-',''),rm.role_id,'perm-user-data-permission','migration-v21'
FROM sys_role_menu rm JOIN sys_menu m ON m.id=rm.menu_id AND m.code='role:grant' AND m.deleted=0
JOIN sys_role r ON r.id=rm.role_id AND r.deleted=0
WHERE rm.deleted=0;
