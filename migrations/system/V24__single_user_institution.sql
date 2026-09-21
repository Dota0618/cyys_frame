-- 一个账号只有一份机构归属和数据范围。多归属旧库由唯一约束拒绝，不能静默选取。
ALTER TABLE sys_user_scope ADD CONSTRAINT uk_user_profile UNIQUE(user_id),
    MODIFY COLUMN scope_id varchar(32) NULL,
    DROP CHECK ck_member_data_range,
    ADD CONSTRAINT ck_member_data_range CHECK(data_range BETWEEN 0 AND 6);

-- 未归属机构的系统账号仍有用户资料；不将管理员伪造为任意机构成员。
INSERT INTO sys_user_scope(id,user_id,scope_id,display_name,status,data_range,created_by,updated_by)
SELECT REPLACE(UUID(),'-',''),u.id,NULL,COALESCE(NULLIF(u.real_name,''),u.login_name),u.status,5,'migration-v24','migration-v24'
FROM sys_user u WHERE u.deleted=0
  AND NOT EXISTS(SELECT 1 FROM sys_user_scope p WHERE p.user_id=u.id);

-- 将原有明确授权的全系统管理员范围保存为用户数据范围，角色不再隐含数据范围。
UPDATE sys_user_scope p JOIN (
    SELECT ur.user_id FROM sys_user_role ur
    JOIN sys_role r ON r.id=ur.role_id AND r.status=1 AND r.deleted=0
    JOIN sys_role_menu rm ON rm.role_id=r.id AND rm.deleted=0
    JOIN sys_menu m ON m.id=rm.menu_id AND m.status=1 AND m.deleted=0
    WHERE ur.deleted=0 AND m.code IN ('platform:scope:access','platform:data:operate')
    GROUP BY ur.user_id HAVING COUNT(DISTINCT m.code)=2
) administrators ON administrators.user_id=p.user_id
SET p.data_range=0,p.version=p.version+1;

ALTER TABLE sys_user_scope DROP CHECK ck_member_role, DROP CHECK ck_member_default,
    DROP COLUMN member_role, DROP COLUMN default_flag, DROP COLUMN expire_at;

UPDATE sys_menu SET status=0,visible=0,version=version+1
WHERE code IN ('platform:scope:access','platform:data:operate','platform:data:read-all');

ALTER TABLE sys_menu DROP CHECK ck_menu_platform, DROP COLUMN platform_only;
