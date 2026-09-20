-- 私有文件元数据。目录、关联与回收入口在消费切片中增加，不保存公共 URL。
CREATE TABLE sys_file_entry (
    id varchar(32) NOT NULL PRIMARY KEY,
    scope_id varchar(32) NOT NULL,
    org_id varchar(32) NULL,
    name varchar(200) NOT NULL,
    storage_profile varchar(64) NOT NULL,
    object_key varchar(32) NOT NULL,
    byte_size bigint NULL,
    sha256 char(64) NULL,
    state varchar(16) NOT NULL,
    expected_bytes bigint NULL,
    part_bytes int NULL,
    expires_at datetime NULL,
    version int NOT NULL DEFAULT 0,
    created_by varchar(32) NOT NULL,
    created_at datetime NOT NULL,
    updated_by varchar(32) NOT NULL,
    updated_at datetime NOT NULL,
    deleted tinyint NOT NULL DEFAULT 0,
    UNIQUE KEY uk_file_object (storage_profile, object_key),
    KEY idx_file_scope_created (scope_id, deleted, created_at, id),
    KEY idx_file_scope_org (scope_id, org_id, deleted),
    CONSTRAINT ck_file_state CHECK (state IN ('UPLOADING','READY','FAILED')),
    CONSTRAINT ck_file_bytes CHECK (byte_size IS NULL OR byte_size >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分片位置在写文件前登记，全部操作先锁定并授权所属文件；不接受客户端对象路径。
CREATE TABLE sys_file_part (
    file_id varchar(32) NOT NULL,
    scope_id varchar(32) NOT NULL,
    actor_id varchar(32) NOT NULL,
    part_index int NOT NULL,
    object_key varchar(32) NOT NULL,
    expected_sha256 char(64) NOT NULL,
    ready tinyint NOT NULL DEFAULT 0,
    PRIMARY KEY (file_id, part_index),
    UNIQUE KEY uk_file_part_key (object_key),
    KEY idx_file_part_scope (scope_id, actor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
