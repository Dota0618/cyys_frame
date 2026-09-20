-- 全局通用配置与字典，保留旧工程业务表名；不含单位归属、凭证种子或部署设置。
CREATE TABLE sys_configure (
    id varchar(32) NOT NULL PRIMARY KEY,
    name varchar(50) NOT NULL,
    `value` varchar(100),
    type tinyint NOT NULL DEFAULT 1,
    desp varchar(255),
    sort int NOT NULL DEFAULT 0,
    version int NOT NULL DEFAULT 0,
    deleted tinyint NOT NULL DEFAULT 0,
    created_by varchar(32),
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32),
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_configure_name (name),
    CONSTRAINT ck_configure_type CHECK (type IN (1,2))
);

CREATE TABLE param_basic (
    id varchar(32) NOT NULL PRIMARY KEY,
    name varchar(20) NOT NULL,
    system_name varchar(255) NOT NULL,
    sort int NOT NULL DEFAULT 0,
    version int NOT NULL DEFAULT 0,
    deleted tinyint NOT NULL DEFAULT 0,
    created_by varchar(32),
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32),
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_param_basic_system_name (system_name)
);

CREATE TABLE param_basic_type (
    id varchar(32) NOT NULL PRIMARY KEY,
    basic_id varchar(32) NOT NULL,
    param_key varchar(20) NOT NULL,
    param_value varchar(20) NOT NULL,
    desp varchar(200),
    sort int NOT NULL DEFAULT 0,
    version int NOT NULL DEFAULT 0,
    deleted tinyint NOT NULL DEFAULT 0,
    created_by varchar(32),
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32),
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_param_basic_type_key (basic_id,param_key),
    KEY idx_param_basic_type_list (basic_id,deleted,sort,id)
);
