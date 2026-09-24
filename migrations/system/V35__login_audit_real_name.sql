ALTER TABLE sys_login_audit
    ADD COLUMN real_name varchar(50) NULL COMMENT '登录时真实姓名' AFTER login_name;
