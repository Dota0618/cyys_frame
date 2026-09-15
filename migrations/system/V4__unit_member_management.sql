-- 单位内成员资料；不改变全局登录账号，不建立数据库外键。
ALTER TABLE sys_user_scope
    ADD COLUMN display_name varchar(100),
    ADD COLUMN version int NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_member_version CHECK (version >= 0);
UPDATE sys_user_scope s JOIN sys_user u ON u.id=s.user_id
SET s.display_name=COALESCE(NULLIF(u.nickname,''),NULLIF(u.real_name,''),u.login_name);
CREATE INDEX idx_member_scope_created ON sys_user_scope(scope_id,created_at,id);
