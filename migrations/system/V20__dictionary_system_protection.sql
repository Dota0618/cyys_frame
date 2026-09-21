-- 系统字典由版本迁移维护；管理 API 创建的分类默认为业务字典。
ALTER TABLE param_basic
    ADD COLUMN system_builtin TINYINT NOT NULL DEFAULT 0 COMMENT '系统内置字典：1只读，0业务可维护',
    ADD CONSTRAINT chk_param_basic_system_builtin CHECK (system_builtin IN (0, 1));

-- 按已核对的固定编码分类保护，不根据中文名称或 SYSTEM 前缀猜测。
-- 机构、版本、系统通知、单位通知类型仍为可维护的业务分类。
UPDATE param_basic
SET system_builtin = 1, version = version + 1, updated_at = CURRENT_TIMESTAMP,
    updated_by = 'migration-v20'
WHERE system_name IN (
    'YES_NO', 'IS_SHOW', 'IS_PUBLISH', 'IS_TOP', 'DISABLE', 'EXAMINE_STATUS',
    'USER_TYPE', 'MENU_TYPE', 'CONFIGURE_TYPE', 'SEX', 'MENU_MODE',
    'QUERY_INPUT_TYPE', 'CACHE_TYPE'
);
