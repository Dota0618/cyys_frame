-- 不假定其他部署的 schema 也使用 general_ci：连接键跟随现有部门表的实际字符规则。
SET @file_collation = (SELECT TABLE_COLLATION FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_org');
SET @file_charset = (SELECT CHARACTER_SET_NAME FROM information_schema.COLLATIONS
    WHERE COLLATION_NAME = @file_collation);
SET @file_ddl = CONCAT('ALTER TABLE sys_file_entry CONVERT TO CHARACTER SET ', @file_charset, ' COLLATE ', @file_collation);
PREPARE file_statement FROM @file_ddl;
EXECUTE file_statement;
DEALLOCATE PREPARE file_statement;
SET @file_ddl = CONCAT('ALTER TABLE sys_file_part CONVERT TO CHARACTER SET ', @file_charset, ' COLLATE ', @file_collation);
PREPARE file_statement FROM @file_ddl;
EXECUTE file_statement;
DEALLOCATE PREPARE file_statement;
