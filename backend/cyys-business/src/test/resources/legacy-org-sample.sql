-- 从只读旧框架 frame_manage.sql 的 sys_org 提取组织字段；不复制账号或凭据。
-- 保留旧 ID、父子关系、type、排序和日期。g1_unknown 是故意构造的未知归属反例。
CREATE TABLE g1_mig_legacy_org (
  id varchar(32) PRIMARY KEY, parent_id varchar(32), name varchar(100),
  type varchar(1), sort int, date_create datetime
);
INSERT INTO g1_mig_legacy_org VALUES
('b566123f944f11ec99f860f2625779f6','0','贵州省文化和旅游厅','1',0,'2018-10-26 17:05:34'),
('b5666780944f11ec99f860f2625779f6','b566123f944f11ec99f860f2625779f6','厅机关各处室','1',1,'2019-04-15 15:34:25'),
('b566437c944f11ec99f860f2625779f6','b5666780944f11ec99f860f2625779f6','办公室','2',2,'2019-04-09 21:24:14');
