-- Rename PENDING to the next unused Flyway version after review. Do not execute from the generator.
CREATE TABLE @@TABLE@@ (
    id varchar(32) NOT NULL PRIMARY KEY COMMENT '记录编号',
    scope_id varchar(32) NOT NULL COMMENT '所属单位编号',
    org_id varchar(32) NOT NULL COMMENT '所属部门编号',
    @@PARENT_SQL@@version int NOT NULL DEFAULT 0 COMMENT '版本号',
    deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    created_by varchar(32) NOT NULL COMMENT '创建人编号',
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by varchar(32) NOT NULL COMMENT '更新人编号',
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
@@SQL_FIELDS@@,
    KEY idx_@@FEATURE@@_scope (scope_id,deleted,created_at,id),
    KEY idx_@@FEATURE@@_org (scope_id,org_id),
    @@PARENT_INDEX@@
    CONSTRAINT ck_@@FEATURE@@_deleted CHECK(deleted IN (0,1))
);
