-- 平台共享地区参数，保留旧 parent/code/name/coordinate/sort 语义。
CREATE TABLE param_region (
 id varchar(32) NOT NULL PRIMARY KEY, parent_id varchar(32),
 city_code varchar(20) NOT NULL, name varchar(20) NOT NULL,
 short_name varchar(20), zip_code varchar(6),
 longitude decimal(18,15), latitude decimal(17,15),
 level int NOT NULL, sort int NOT NULL DEFAULT 0,
 version int NOT NULL DEFAULT 0, deleted tinyint NOT NULL DEFAULT 0,
 created_by varchar(32) NOT NULL, created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_by varchar(32) NOT NULL, updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uk_region_code(city_code),
 KEY idx_region_parent(parent_id,deleted,sort,id),
 CONSTRAINT ck_region_level CHECK(level>=0),
 CONSTRAINT ck_region_longitude CHECK(longitude BETWEEN -180 AND 180),
 CONSTRAINT ck_region_latitude CHECK(latitude BETWEEN -90 AND 90),
 CONSTRAINT ck_region_deleted CHECK(deleted IN (0,1))
);
