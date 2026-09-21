-- 单位维护的乐观锁版本；菜单目录数据使用 migrations/dev-data/menu-tree.sql 显式初始化。
ALTER TABLE sys_scope ADD COLUMN version int NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_scope_version CHECK (version >= 0);
