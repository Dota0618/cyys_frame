-- 新通用取号存储，不直接导入旧 sys_serial_number 的不明全局计数。
-- 规则首次使用后不可原地改变；旧周期和序号不能删除或回零后重新使用。
CREATE TABLE sys_serial_rule (
 scope_id varchar(32) NOT NULL,
 business_code varchar(32) NOT NULL,
 prefix varchar(32) NOT NULL,
 period_type varchar(8) NOT NULL,
 width int NOT NULL,
 zone_id varchar(64) NOT NULL,
 permission varchar(100) NOT NULL,
 created_by varchar(32) NOT NULL,
 created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY (scope_id,business_code),
 UNIQUE KEY uk_serial_prefix (scope_id,prefix),
 CONSTRAINT ck_serial_rule_scope CHECK(scope_id<>'' AND scope_id<>'GLOBAL'),
 CONSTRAINT ck_serial_rule_width CHECK(width BETWEEN 1 AND 18),
 CONSTRAINT ck_serial_rule_period CHECK(period_type IN ('NONE','DAY','MONTH','YEAR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE sys_serial_counter (
 id varchar(32) PRIMARY KEY,
 scope_id varchar(32) NOT NULL,
 business_code varchar(32) NOT NULL,
 period_key varchar(16) NOT NULL,
 sequence_value bigint NOT NULL DEFAULT 0,
 updated_by varchar(32) NOT NULL,
 updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_serial_period (scope_id,business_code,period_key),
 CONSTRAINT ck_serial_counter_scope CHECK(scope_id<>'' AND scope_id<>'GLOBAL'),
 CONSTRAINT ck_serial_counter_value CHECK(sequence_value BETWEEN 0 AND 999999999999999999)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
