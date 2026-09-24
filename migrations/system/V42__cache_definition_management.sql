CREATE TABLE sys_cache_definition (
    id varchar(32) NOT NULL PRIMARY KEY,
    name varchar(100) NOT NULL,
    cache_key varchar(100) NOT NULL,
    group_name varchar(50) NOT NULL,
    refresh_group varchar(50) NOT NULL,
    scope varchar(10) NOT NULL,
    description varchar(255),
    version int NOT NULL DEFAULT 0,
    deleted tinyint NOT NULL DEFAULT 0,
    created_by varchar(32),
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by varchar(32),
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cache_definition_key (cache_key),
    KEY idx_cache_definition_group (group_name,refresh_group,scope,deleted),
    CONSTRAINT ck_cache_definition_scope CHECK (scope IN ('SYSTEM', 'UNIT')),
    CONSTRAINT ck_cache_definition_deleted CHECK (deleted IN (0, 1))
);

INSERT INTO sys_cache_definition
    (id,name,cache_key,group_name,refresh_group,scope,description,created_by,updated_by)
SELECT CONCAT('legacy-', LEFT(id, 25)), name, name, '独立缓存', '独立刷新', 'SYSTEM',
       '由旧刷新配置迁移；请按实际缓存区域修改。', 'migration-v42', 'migration-v42'
FROM sys_cache_refresh
WHERE deleted=0;

INSERT INTO sys_cache_definition
    (id,name,cache_key,group_name,refresh_group,scope,description,created_by,updated_by)
SELECT 'cache-user-records', '用户缓存', 'user-records', '用户', '用户缓存', 'SYSTEM',
       '用户展示记录（ID、姓名、状态，含停用用户）。', 'migration-v42', 'migration-v42'
WHERE NOT EXISTS (SELECT 1 FROM sys_cache_definition WHERE cache_key='user-records');

INSERT INTO sys_cache_definition
    (id,name,cache_key,group_name,refresh_group,scope,description,created_by,updated_by)
SELECT 'cache-scope-records', '单位缓存', 'scope-records', '单位', '单位缓存', 'SYSTEM',
       '单位展示记录（ID、名称、简称，含停用单位）。', 'migration-v42', 'migration-v42'
WHERE NOT EXISTS (SELECT 1 FROM sys_cache_definition WHERE cache_key='scope-records');

INSERT INTO sys_cache_definition
    (id,name,cache_key,group_name,refresh_group,scope,description,created_by,updated_by)
SELECT 'cache-department-records', '部门缓存', 'department-records', '部门', '部门缓存', 'UNIT',
       '部门展示记录（ID、名称）。', 'migration-v42', 'migration-v42'
WHERE NOT EXISTS (SELECT 1 FROM sys_cache_definition WHERE cache_key='department-records');

INSERT INTO sys_cache_definition
    (id,name,cache_key,group_name,refresh_group,scope,description,created_by,updated_by)
SELECT 'cache-dictionary-options', '字典选项缓存', 'dictionary-options', '字典', '字典缓存', 'SYSTEM',
       '字典有效选项列表。', 'migration-v42', 'migration-v42'
WHERE NOT EXISTS (SELECT 1 FROM sys_cache_definition WHERE cache_key='dictionary-options');

INSERT INTO sys_cache_definition
    (id,name,cache_key,group_name,refresh_group,scope,description,created_by,updated_by)
SELECT 'cache-dictionary-labels', '字典项缓存', 'dictionary-labels', '字典', '字典缓存', 'SYSTEM',
       '字典项名称与失效状态。', 'migration-v42', 'migration-v42'
WHERE NOT EXISTS (SELECT 1 FROM sys_cache_definition WHERE cache_key='dictionary-labels');

INSERT INTO sys_cache_definition
    (id,name,cache_key,group_name,refresh_group,scope,description,created_by,updated_by)
SELECT 'cache-configuration-values', '全局参数缓存', 'configuration-values', '全局参数', '全局参数缓存', 'SYSTEM',
       '全局参数值。', 'migration-v42', 'migration-v42'
WHERE NOT EXISTS (SELECT 1 FROM sys_cache_definition WHERE cache_key='configuration-values');

DROP TABLE sys_cache_refresh;
