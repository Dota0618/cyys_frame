-- 同一管理体系只有一个实际总单位。已有多根库明确拒绝，不任意选取或改写业务归属。
ALTER TABLE sys_scope
    ADD COLUMN root_slot tinyint GENERATED ALWAYS AS
        (CASE WHEN deleted=0 AND parent_id='0' THEN 1 ELSE NULL END) STORED,
    ADD UNIQUE KEY uk_scope_single_root(root_slot),
    ADD CONSTRAINT ck_scope_root_enabled CHECK(deleted=1 OR parent_id<>'0' OR status=1);

-- 原无机构归属的全系统账号归属实际总单位；权限、账号和业务记录归属保持不变。
UPDATE sys_user_scope p
JOIN sys_scope root ON root.parent_id='0' AND root.deleted=0 AND root.status=1
SET p.scope_id=root.id,p.version=p.version+1,p.updated_by='migration-v26',p.updated_at=CURRENT_TIMESTAMP
WHERE p.scope_id IS NULL AND p.data_range=0 AND p.deleted=0;
