-- Phase 2 空库结构，唯一执行入口为应用中的 Flyway。
-- 无删表、无账号密码和开发种子。已有未纳管数据库由 baseline-on-migrate=false 拒绝。
-- sys_user 为全局账号；sys_scope、sys_menu 为受保护的平台/共享目录。
-- sys_role 的 GLOBAL 仅标识平台角色；sys_role_menu 归属由角色推导。
-- Phase 3 尚未实现全部行级隔离，此结构不代表 G1 已通过。

CREATE TABLE sys_scope (
    id varchar(32) NOT NULL PRIMARY KEY,
    code varchar(64) NOT NULL,
    name varchar(100) NOT NULL,
    short_name varchar(50),
    status tinyint NOT NULL DEFAULT 1,
    contact_person varchar(50),
    contact_phone varchar(20),
    region_code varchar(12),
    sort int NOT NULL DEFAULT 0,
    remark varchar(500),
    created_by varchar(32),
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32),
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted tinyint NOT NULL DEFAULT 0,
    CONSTRAINT uk_scope_code UNIQUE (code),
    CONSTRAINT ck_scope_owner CHECK (id <> 'GLOBAL'),
    CONSTRAINT ck_scope_status CHECK (status IN (0, 1)),
    CONSTRAINT ck_sys_scope_deleted CHECK (deleted IN (0, 1))
);

CREATE TABLE sys_user (
    id varchar(32) NOT NULL PRIMARY KEY,
    login_name varchar(64) NOT NULL,
    password_hash varchar(255) NOT NULL,
    nickname varchar(50),
    real_name varchar(50),
    avatar varchar(500),
    phone varchar(20),
    email varchar(100),
    id_card varchar(18),
    status tinyint NOT NULL DEFAULT 1,
    pwd_error_count int NOT NULL DEFAULT 0,
    pwd_lock_time datetime,
    pwd_last_changed datetime,
    last_login_time datetime,
    last_login_ip varchar(50),
    created_by varchar(32),
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32),
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted tinyint NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_login_name UNIQUE (login_name),
    CONSTRAINT ck_user_status CHECK (status IN (0, 1)),
    CONSTRAINT ck_user_error_count CHECK (pwd_error_count >= 0),
    CONSTRAINT ck_sys_user_deleted CHECK (deleted IN (0, 1))
);

CREATE TABLE sys_user_scope (
    id varchar(32) NOT NULL PRIMARY KEY,
    user_id varchar(32) NOT NULL,
    scope_id varchar(32) NOT NULL,
    member_role varchar(20) NOT NULL DEFAULT 'MEMBER',
    default_flag tinyint NOT NULL DEFAULT 0,
    status tinyint NOT NULL DEFAULT 1,
    expire_at datetime,
    created_by varchar(32),
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32),
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted tinyint NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_scope UNIQUE (user_id, scope_id),
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_member_scope FOREIGN KEY (scope_id) REFERENCES sys_scope (id),
    CONSTRAINT ck_member_role CHECK (member_role IN ('OWNER', 'ADMIN', 'MEMBER')),
    CONSTRAINT ck_member_status CHECK (status IN (0, 1)),
    CONSTRAINT ck_member_default CHECK (default_flag IN (0, 1)),
    CONSTRAINT ck_sys_user_scope_deleted CHECK (deleted IN (0, 1))
);

CREATE TABLE sys_role (
    id varchar(32) NOT NULL PRIMARY KEY,
    scope_id varchar(32) NOT NULL,
    code varchar(64) NOT NULL,
    name varchar(50) NOT NULL,
    data_range tinyint NOT NULL DEFAULT 5,
    custom_orgs varchar(4000),
    status tinyint NOT NULL DEFAULT 1,
    sort int NOT NULL DEFAULT 0,
    remark varchar(500),
    created_by varchar(32),
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32),
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted tinyint NOT NULL DEFAULT 0,
    CONSTRAINT uk_role_scope_code UNIQUE (scope_id, code),
    CONSTRAINT uk_role_id_scope UNIQUE (id, scope_id),
    CONSTRAINT ck_role_status CHECK (status IN (0, 1)),
    CONSTRAINT ck_sys_role_deleted CHECK (deleted IN (0, 1))
);

CREATE TABLE sys_user_role (
    id varchar(32) NOT NULL PRIMARY KEY,
    user_id varchar(32) NOT NULL,
    role_id varchar(32) NOT NULL,
    scope_id varchar(32) NOT NULL,
    created_by varchar(32),
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32),
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted tinyint NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id, scope_id),
    CONSTRAINT fk_grant_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_grant_role_scope FOREIGN KEY (role_id, scope_id) REFERENCES sys_role (id, scope_id),
    CONSTRAINT ck_sys_user_role_deleted CHECK (deleted IN (0, 1))
);

CREATE TABLE sys_menu (
    id varchar(32) NOT NULL PRIMARY KEY,
    parent_id varchar(32) NOT NULL DEFAULT '0',
    name varchar(50) NOT NULL,
    code varchar(100) NOT NULL,
    path varchar(255),
    component varchar(255),
    redirect varchar(255),
    icon varchar(100),
    type tinyint NOT NULL DEFAULT 2,
    sort int NOT NULL DEFAULT 0,
    visible tinyint NOT NULL DEFAULT 1,
    status tinyint NOT NULL DEFAULT 1,
    platform_only tinyint NOT NULL DEFAULT 0,
    keep_alive tinyint NOT NULL DEFAULT 0,
    always_show tinyint NOT NULL DEFAULT 0,
    created_by varchar(32),
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32),
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted tinyint NOT NULL DEFAULT 0,
    CONSTRAINT uk_menu_code UNIQUE (code),
    CONSTRAINT ck_menu_type CHECK (type IN (1, 2, 3)),
    CONSTRAINT ck_menu_status CHECK (status IN (0, 1)),
    CONSTRAINT ck_menu_platform CHECK (platform_only IN (0, 1)),
    CONSTRAINT ck_sys_menu_deleted CHECK (deleted IN (0, 1))
);

CREATE TABLE sys_role_menu (
    id varchar(32) NOT NULL PRIMARY KEY,
    role_id varchar(32) NOT NULL,
    menu_id varchar(32) NOT NULL,
    created_by varchar(32),
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32),
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted tinyint NOT NULL DEFAULT 0,
    CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id),
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role (id),
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu (id),
    CONSTRAINT ck_sys_role_menu_deleted CHECK (deleted IN (0, 1))
);

CREATE TABLE sys_org (
    id varchar(32) NOT NULL PRIMARY KEY,
    scope_id varchar(32) NOT NULL,
    parent_id varchar(32) NOT NULL DEFAULT '0',
    ancestors varchar(500) NOT NULL DEFAULT '',
    name varchar(100) NOT NULL,
    code varchar(64),
    leader_id varchar(32),
    phone varchar(20),
    email varchar(100),
    sort int NOT NULL DEFAULT 0,
    status tinyint NOT NULL DEFAULT 1,
    created_by varchar(32),
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32),
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted tinyint NOT NULL DEFAULT 0,
    CONSTRAINT fk_org_scope FOREIGN KEY (scope_id) REFERENCES sys_scope (id),
    CONSTRAINT ck_org_status CHECK (status IN (0, 1)),
    CONSTRAINT ck_sys_org_deleted CHECK (deleted IN (0, 1))
);

CREATE INDEX idx_scope_member ON sys_user_scope (scope_id, status);
CREATE INDEX idx_scope_role ON sys_user_role (scope_id, role_id);
CREATE INDEX idx_menu_parent ON sys_menu (parent_id, sort);
CREATE INDEX idx_org_scope_parent ON sys_org (scope_id, parent_id);
