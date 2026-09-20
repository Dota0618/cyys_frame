CREATE TABLE sys_version (
 id varchar(32) NOT NULL PRIMARY KEY, release_number varchar(10) NOT NULL,
 date_update date NOT NULL, content varchar(1000) NOT NULL,
 type varchar(20) NOT NULL, type_label varchar(20) NOT NULL,
 version int NOT NULL DEFAULT 0, deleted tinyint NOT NULL DEFAULT 0,
 created_by varchar(32) NOT NULL, created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_by varchar(32) NOT NULL, updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_version_created(deleted,created_at,id)
);
-- 关联的业务范围与源文件归属分别保存；不把单位文件改为 GLOBAL 文件。
ALTER TABLE sys_file_binding ADD COLUMN file_scope_id varchar(32) NULL;
UPDATE sys_file_binding SET file_scope_id=scope_id;
ALTER TABLE sys_file_binding MODIFY COLUMN file_scope_id varchar(32) NOT NULL,
 DROP CHECK ck_file_binding_resource,
 ADD CONSTRAINT ck_file_binding_resource CHECK(resource IN ('ORG_NOTICE','RELEASE')),
 ADD CONSTRAINT ck_file_binding_owner CHECK(
   (resource='ORG_NOTICE' AND scope_id=file_scope_id) OR (resource='RELEASE' AND scope_id='GLOBAL')),
 DROP INDEX idx_file_binding_file,
 ADD KEY idx_file_binding_file(file_scope_id,file_id);
SET @release_charset=(SELECT CHARACTER_SET_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_org' AND COLUMN_NAME='id');
SET @release_collation=(SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_org' AND COLUMN_NAME='id');
SET @release_sql=CONCAT('ALTER TABLE sys_file_binding CONVERT TO CHARACTER SET ',@release_charset,' COLLATE ',@release_collation);
PREPARE release_stmt FROM @release_sql; EXECUTE release_stmt; DEALLOCATE PREPARE release_stmt;
SET @release_sql=CONCAT('ALTER TABLE sys_version CONVERT TO CHARACTER SET ',@release_charset,' COLLATE ',@release_collation);
PREPARE release_stmt FROM @release_sql; EXECUTE release_stmt; DEALLOCATE PREPARE release_stmt;
