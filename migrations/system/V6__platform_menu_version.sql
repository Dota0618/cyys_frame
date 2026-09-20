-- 共享菜单定义使用显式版本条件，避免并发覆盖权限和路由元数据。
ALTER TABLE sys_menu
    ADD COLUMN version int NOT NULL DEFAULT 0 AFTER sort,
    ADD CONSTRAINT ck_menu_version CHECK (version >= 0);
