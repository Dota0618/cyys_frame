-- 角色管理写入使用显式版本条件，避免并发覆盖菜单与数据范围配置。
ALTER TABLE sys_role
    ADD COLUMN version int NOT NULL DEFAULT 0 AFTER sort;
