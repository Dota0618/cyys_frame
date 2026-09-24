INSERT INTO param_basic(id,name,system_name,sort,system_builtin,created_by,updated_by)
VALUES ('login-audit-outcome','登录记录结果','LOGIN_AUDIT_OUTCOME',0,1,'migration-v36','migration-v36');

INSERT INTO param_basic_type(id,basic_id,param_key,param_value,sort,created_by,updated_by)
VALUES
    ('login-audit-outcome-success','login-audit-outcome','SUCCESS','成功',0,'migration-v36','migration-v36'),
    ('login-audit-outcome-failure','login-audit-outcome','FAILURE','失败',1,'migration-v36','migration-v36'),
    ('login-audit-outcome-locked','login-audit-outcome','LOCKED','锁定',2,'migration-v36','migration-v36'),
    ('login-audit-outcome-disabled','login-audit-outcome','DISABLED','已禁用',3,'migration-v36','migration-v36');
