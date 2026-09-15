-- Phase 3 增量升级。既有 V1 保持历史校验值，外键在本版移除。
-- 先检查旧范围值；不把未转换的部门串默认为全部范围。
ALTER TABLE sys_role ADD CONSTRAINT ck_role_legacy_orgs CHECK (custom_orgs IS NULL OR custom_orgs = '');
ALTER TABLE sys_role ADD CONSTRAINT ck_role_data_range CHECK (data_range BETWEEN 1 AND 5);
ALTER TABLE sys_user_scope DROP FOREIGN KEY fk_member_user, DROP FOREIGN KEY fk_member_scope;
ALTER TABLE sys_user_role DROP FOREIGN KEY fk_grant_user, DROP FOREIGN KEY fk_grant_role_scope;
ALTER TABLE sys_role_menu DROP FOREIGN KEY fk_role_menu_role, DROP FOREIGN KEY fk_role_menu_menu;
ALTER TABLE sys_org DROP FOREIGN KEY fk_org_scope;

-- 关联关系及写入校验职责见 docs/development/backend/database-query.md 和 migrations/README.md。

ALTER TABLE sys_org MODIFY parent_id varchar(32) NULL DEFAULT NULL;
UPDATE sys_org SET parent_id = NULL WHERE parent_id = '0';
ALTER TABLE sys_org ADD COLUMN version int NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_org_version CHECK (version >= 0),
    ADD CONSTRAINT uk_org_id_scope UNIQUE (id, scope_id),
    ADD CONSTRAINT uk_org_scope_code UNIQUE (scope_id, code),
    ADD CONSTRAINT ck_org_not_self_parent CHECK (parent_id IS NULL OR parent_id <> id);
CREATE INDEX idx_org_parent_scope ON sys_org(parent_id, scope_id);

ALTER TABLE sys_user_scope ADD COLUMN org_id varchar(32);
CREATE INDEX idx_member_org_scope ON sys_user_scope(org_id, scope_id);

CREATE TABLE sys_role_org (
    role_id varchar(32) NOT NULL,
    scope_id varchar(32) NOT NULL,
    org_id varchar(32) NOT NULL,
    PRIMARY KEY (role_id, org_id)
);
CREATE INDEX idx_role_org_scope ON sys_role_org(scope_id, org_id);
ALTER TABLE sys_role DROP CHECK ck_role_legacy_orgs, DROP COLUMN custom_orgs;

CREATE TABLE sys_data_audit (
    id varchar(32) NOT NULL PRIMARY KEY,
    actor_id varchar(32) NOT NULL,
    scope_id varchar(32) NOT NULL,
    resource varchar(64) NOT NULL,
    record_id varchar(32) NOT NULL,
    operation varchar(20) NOT NULL,
    authorized_view varchar(32) NOT NULL,
    outcome varchar(20) NOT NULL,
    occurred_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_audit_outcome CHECK (outcome = 'SUCCESS')
);
CREATE INDEX idx_audit_scope_time ON sys_data_audit(scope_id, occurred_at);
