CREATE TABLE sys_cache_refresh (
    id varchar(32) NOT NULL PRIMARY KEY,
    name varchar(50) NOT NULL,
    version int NOT NULL DEFAULT 0,
    deleted tinyint NOT NULL DEFAULT 0,
    created_by varchar(32),
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32),
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cache_refresh_name (name),
    CONSTRAINT ck_cache_refresh_deleted CHECK (deleted IN (0, 1))
);
