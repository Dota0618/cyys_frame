ALTER TABLE sys_data_audit
    CHANGE COLUMN actor_id user_id varchar(32) NOT NULL COMMENT '操作用户编号';

INSERT INTO param_basic(id,name,system_name,sort,system_builtin,created_by,updated_by)
VALUES ('data-audit-operation','操作审计操作','DATA_AUDIT_OPERATION',0,1,'migration-v37','migration-v37');

INSERT INTO param_basic_type(id,basic_id,param_key,param_value,sort,created_by,updated_by)
VALUES
    ('data-audit-operation-create','data-audit-operation','CREATE','新增',0,'migration-v37','migration-v37'),
    ('data-audit-operation-update','data-audit-operation','UPDATE','修改',1,'migration-v37','migration-v37'),
    ('data-audit-operation-delete','data-audit-operation','DELETE','删除',2,'migration-v37','migration-v37'),
    ('data-audit-operation-replace','data-audit-operation','REPLACE','替换',3,'migration-v37','migration-v37'),
    ('data-audit-operation-refresh','data-audit-operation','REFRESH','刷新',4,'migration-v37','migration-v37'),
    ('data-audit-operation-export','data-audit-operation','EXPORT_GENERATED','生成导出',5,'migration-v37','migration-v37'),
    ('data-audit-operation-generate','data-audit-operation','GENERATE','生成',6,'migration-v37','migration-v37'),
    ('data-audit-op-change-pwd','data-audit-operation','CHANGE_PASSWORD','修改密码',7,'migration-v37','migration-v37'),
    ('data-audit-op-reset-pwd','data-audit-operation','RESET_PASSWORD','重置密码',8,'migration-v37','migration-v37'),
    ('data-audit-operation-grant','data-audit-operation','GRANT','授予角色',9,'migration-v37','migration-v37'),
    ('data-audit-operation-upload','data-audit-operation','UPLOAD','上传',10,'migration-v37','migration-v37'),
    ('data-audit-op-cancel-upload','data-audit-operation','CANCEL_UPLOAD','取消上传',11,'migration-v37','migration-v37'),
    ('data-audit-operation-move','data-audit-operation','MOVE','移动',12,'migration-v37','migration-v37'),
    ('data-audit-operation-restore','data-audit-operation','RESTORE','恢复',13,'migration-v37','migration-v37'),
    ('data-audit-operation-purge','data-audit-operation','PURGE','彻底删除',14,'migration-v37','migration-v37'),
    ('data-audit-op-expire-upload','data-audit-operation','EXPIRE_UPLOAD','上传过期',15,'migration-v37','migration-v37'),
    ('data-audit-op-clean-upload','data-audit-operation','CLEAN_UPLOAD','清理上传',16,'migration-v37','migration-v37'),
    ('data-audit-operation-reserve','data-audit-operation','RESERVE','预留流水号',17,'migration-v37','migration-v37');

INSERT INTO param_basic(id,name,system_name,sort,system_builtin,created_by,updated_by)
VALUES ('data-audit-outcome','操作审计结果','DATA_AUDIT_OUTCOME',0,1,'migration-v37','migration-v37');

INSERT INTO param_basic_type(id,basic_id,param_key,param_value,sort,created_by,updated_by)
VALUES ('data-audit-outcome-success','data-audit-outcome','SUCCESS','成功',0,'migration-v37','migration-v37');
