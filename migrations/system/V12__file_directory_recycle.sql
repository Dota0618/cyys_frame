CREATE TABLE sys_file_dir (
    id varchar(32) NOT NULL PRIMARY KEY,
    scope_id varchar(32) NOT NULL,
    org_id varchar(32),
    name varchar(100) NOT NULL,
    version int NOT NULL DEFAULT 0,
    created_by varchar(32) NOT NULL,
    created_at datetime NOT NULL,
    updated_by varchar(32) NOT NULL,
    updated_at datetime NOT NULL,
    deleted tinyint NOT NULL DEFAULT 0,
    KEY idx_file_dir_owner (scope_id, created_by, deleted, created_at, id)
);
SET @directory_collation = (SELECT TABLE_COLLATION FROM information_schema.TABLES
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_file_entry');
SET @directory_charset = (SELECT CHARACTER_SET_NAME FROM information_schema.COLLATIONS
    WHERE COLLATION_NAME=@directory_collation);
SET @directory_ddl = CONCAT('ALTER TABLE sys_file_dir CONVERT TO CHARACTER SET ', @directory_charset, ' COLLATE ', @directory_collation);
PREPARE directory_statement FROM @directory_ddl;
EXECUTE directory_statement;
DEALLOCATE PREPARE directory_statement;
ALTER TABLE sys_file_entry
    ADD COLUMN dir_id varchar(32) NULL,
    ADD KEY idx_file_directory (scope_id, dir_id, created_by, deleted),
    DROP CHECK ck_file_state,
    ADD CONSTRAINT ck_file_state CHECK (state IN ('UPLOADING','READY','FAILED','PURGING','PURGED'));
-- 新增列继承原表实际字符规则，目录只是逻辑分类，不改变物理存储位置。
