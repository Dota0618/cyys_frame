CREATE TABLE sys_org_notice (
 id varchar(32) NOT NULL PRIMARY KEY, scope_id varchar(32) NOT NULL, org_id varchar(32) NOT NULL,
 title varchar(100) NOT NULL, type varchar(64) NULL, type_label varchar(200) NULL, content varchar(2000) NOT NULL,
 version int NOT NULL DEFAULT 0, created_by varchar(32) NOT NULL, created_at datetime NOT NULL,
 updated_by varchar(32) NOT NULL, updated_at datetime NOT NULL, deleted tinyint NOT NULL DEFAULT 0,
 KEY idx_notice_scope_created(scope_id,deleted,created_at,id), KEY idx_notice_org(scope_id,org_id,deleted)
) ENGINE=InnoDB;
CREATE TABLE sys_org_notice_ref (
 scope_id varchar(32) NOT NULL, notice_id varchar(32) NOT NULL, user_id varchar(32) NOT NULL,
 first_read_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(scope_id,notice_id,user_id), KEY idx_notice_reads(scope_id,notice_id,first_read_at,user_id)
) ENGINE=InnoDB;
CREATE TABLE sys_file_binding (
 scope_id varchar(32) NOT NULL, resource varchar(32) NOT NULL, record_id varchar(32) NOT NULL,
 file_id varchar(32) NOT NULL, sort int NOT NULL, created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(scope_id,resource,record_id,file_id), KEY idx_file_binding_file(scope_id,file_id),
 CONSTRAINT ck_file_binding_resource CHECK(resource IN ('ORG_NOTICE'))
) ENGINE=InnoDB;
-- 跟随当前身份表的字符规则；不建立外键，父记录与文件锁协调绑定/删除。
SET @notice_charset=(SELECT CHARACTER_SET_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_org' AND COLUMN_NAME='id');
SET @notice_collation=(SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_org' AND COLUMN_NAME='id');
SET @notice_sql=CONCAT('ALTER TABLE sys_org_notice CONVERT TO CHARACTER SET ',@notice_charset,' COLLATE ',@notice_collation);
PREPARE notice_stmt FROM @notice_sql; EXECUTE notice_stmt; DEALLOCATE PREPARE notice_stmt;
SET @notice_sql=CONCAT('ALTER TABLE sys_org_notice_ref CONVERT TO CHARACTER SET ',@notice_charset,' COLLATE ',@notice_collation);
PREPARE notice_stmt FROM @notice_sql; EXECUTE notice_stmt; DEALLOCATE PREPARE notice_stmt;
SET @notice_sql=CONCAT('ALTER TABLE sys_file_binding CONVERT TO CHARACTER SET ',@notice_charset,' COLLATE ',@notice_collation);
PREPARE notice_stmt FROM @notice_sql; EXECUTE notice_stmt; DEALLOCATE PREPARE notice_stmt;
