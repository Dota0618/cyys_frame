-- 登录记录只保存认证结果所需的最小字段，不保存密码、令牌或原始请求参数。
CREATE TABLE sys_login_audit (
    id varchar(32) NOT NULL PRIMARY KEY,
    login_name varchar(128) NOT NULL,
    user_id varchar(32) NULL,
    scope_id varchar(32) NULL,
    client_ip varchar(64) NULL,
    outcome varchar(20) NOT NULL,
    reason varchar(64) NULL,
    occurred_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_login_audit_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE', 'LOCKED', 'DISABLED'))
);
CREATE INDEX idx_login_audit_scope_time ON sys_login_audit(scope_id, occurred_at, id);
CREATE INDEX idx_login_audit_name_time ON sys_login_audit(login_name, occurred_at, id);
