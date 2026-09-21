-- 同一管理体系下的机构树；已有机构保留原 ID 和业务归属，作为根级机构。
ALTER TABLE sys_scope
    ADD COLUMN parent_id varchar(32) NOT NULL DEFAULT '0',
    ADD COLUMN ancestors varchar(500) NOT NULL DEFAULT '',
    ADD INDEX idx_scope_parent(parent_id, sort);

UPDATE sys_menu SET name='机构管理',version=version+1 WHERE code='nav:menu:scopes';
-- 生成器暂停，对应代码保留但入口不开放。
UPDATE sys_menu SET status=0,version=version+1
 WHERE code IN ('nav:menu:generator','generator:read','generator:generate');
