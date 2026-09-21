-- Rename PENDING to the next unused Flyway version after review. Do not execute from the generator.
CREATE TABLE @@TABLE@@ (
    id varchar(32) NOT NULL PRIMARY KEY,
    scope_id varchar(32) NOT NULL,
    org_id varchar(32) NOT NULL,
    @@PARENT_SQL@@version int NOT NULL DEFAULT 0,
    deleted tinyint NOT NULL DEFAULT 0,
    created_by varchar(32) NOT NULL,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32) NOT NULL,
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
@@SQL_FIELDS@@,
    KEY idx_@@FEATURE@@_scope (scope_id,deleted,created_at,id),
    KEY idx_@@FEATURE@@_org (scope_id,org_id),
    @@PARENT_INDEX@@
    CONSTRAINT ck_@@FEATURE@@_deleted CHECK(deleted IN (0,1))
);
