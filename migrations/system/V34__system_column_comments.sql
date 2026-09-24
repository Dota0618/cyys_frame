-- 为系统表的全部字段补充中文说明；不改动字段类型、默认值及约束。

ALTER TABLE sys_scope
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '单位编号',
    MODIFY COLUMN code varchar(64) NOT NULL COMMENT '单位编码',
    MODIFY COLUMN name varchar(100) NOT NULL COMMENT '单位名称',
    MODIFY COLUMN short_name varchar(50) NULL COMMENT '单位简称',
    MODIFY COLUMN status tinyint NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    MODIFY COLUMN contact_person varchar(50) NULL COMMENT '联系人',
    MODIFY COLUMN contact_phone varchar(20) NULL COMMENT '联系电话',
    MODIFY COLUMN region_code varchar(12) NULL COMMENT '地区编码',
    MODIFY COLUMN sort int NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN remark varchar(500) NULL COMMENT '备注',
    MODIFY COLUMN created_by varchar(32) NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号',
    MODIFY COLUMN parent_id varchar(32) NOT NULL DEFAULT '0' COMMENT '上级单位编号',
    MODIFY COLUMN ancestors varchar(500) NOT NULL DEFAULT '' COMMENT '上级单位链路',
    MODIFY COLUMN root_slot tinyint GENERATED ALWAYS AS (CASE WHEN deleted=0 AND parent_id='0' THEN 1 ELSE NULL END) STORED COMMENT '根单位占位标记';

ALTER TABLE sys_user
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '用户编号',
    MODIFY COLUMN login_name varchar(64) NOT NULL COMMENT '登录名',
    MODIFY COLUMN password_hash varchar(255) NOT NULL COMMENT '密码哈希',
    MODIFY COLUMN nickname varchar(50) NULL COMMENT '昵称',
    MODIFY COLUMN real_name varchar(50) NULL COMMENT '真实姓名',
    MODIFY COLUMN avatar varchar(500) NULL COMMENT '头像地址',
    MODIFY COLUMN phone varchar(20) NULL COMMENT '手机号',
    MODIFY COLUMN email varchar(100) NULL COMMENT '电子邮箱',
    MODIFY COLUMN id_card varchar(18) NULL COMMENT '身份证号',
    MODIFY COLUMN status tinyint NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    MODIFY COLUMN pwd_error_count int NOT NULL DEFAULT 0 COMMENT '密码错误次数',
    MODIFY COLUMN pwd_lock_time datetime NULL COMMENT '密码锁定时间',
    MODIFY COLUMN pwd_last_changed datetime NULL COMMENT '密码最后修改时间',
    MODIFY COLUMN last_login_time datetime NULL COMMENT '最后登录时间',
    MODIFY COLUMN last_login_ip varchar(50) NULL COMMENT '最后登录IP',
    MODIFY COLUMN created_by varchar(32) NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除';

ALTER TABLE sys_user_scope
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '用户资料编号',
    MODIFY COLUMN user_id varchar(32) NOT NULL COMMENT '用户编号',
    MODIFY COLUMN scope_id varchar(32) NULL COMMENT '所属单位编号',
    MODIFY COLUMN status tinyint NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    MODIFY COLUMN created_by varchar(32) NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    MODIFY COLUMN org_id varchar(32) NULL COMMENT '所属部门编号',
    MODIFY COLUMN display_name varchar(100) NULL COMMENT '单位内显示名称',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号',
    MODIFY COLUMN data_range tinyint NOT NULL DEFAULT 5 COMMENT '数据权限范围编码';

ALTER TABLE sys_role
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '角色编号',
    MODIFY COLUMN code varchar(64) NOT NULL COMMENT '角色编码',
    MODIFY COLUMN name varchar(50) NOT NULL COMMENT '角色名称',
    MODIFY COLUMN status tinyint NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    MODIFY COLUMN sort int NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN remark varchar(500) NULL COMMENT '备注',
    MODIFY COLUMN created_by varchar(32) NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号';

ALTER TABLE sys_user_role
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '用户角色关系编号',
    MODIFY COLUMN user_id varchar(32) NOT NULL COMMENT '用户编号',
    MODIFY COLUMN role_id varchar(32) NOT NULL COMMENT '角色编号',
    MODIFY COLUMN created_by varchar(32) NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除';

ALTER TABLE sys_menu
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '菜单编号',
    MODIFY COLUMN parent_id varchar(32) NOT NULL DEFAULT '0' COMMENT '父菜单编号',
    MODIFY COLUMN name varchar(50) NOT NULL COMMENT '菜单名称',
    MODIFY COLUMN code varchar(100) NOT NULL COMMENT '菜单编码',
    MODIFY COLUMN path varchar(255) NULL COMMENT '路由路径',
    MODIFY COLUMN component varchar(255) NULL COMMENT '组件路径',
    MODIFY COLUMN redirect varchar(255) NULL COMMENT '重定向路径',
    MODIFY COLUMN icon varchar(100) NULL COMMENT '图标名称',
    MODIFY COLUMN type tinyint NOT NULL DEFAULT 2 COMMENT '菜单类型：1目录，2菜单，3按钮',
    MODIFY COLUMN sort int NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN visible tinyint NOT NULL DEFAULT 1 COMMENT '是否显示：0否，1是',
    MODIFY COLUMN status tinyint NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    MODIFY COLUMN keep_alive tinyint NOT NULL DEFAULT 0 COMMENT '是否缓存页面：0否，1是',
    MODIFY COLUMN always_show tinyint NOT NULL DEFAULT 0 COMMENT '是否始终显示：0否，1是',
    MODIFY COLUMN created_by varchar(32) NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号';

ALTER TABLE sys_role_menu
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '角色菜单关系编号',
    MODIFY COLUMN role_id varchar(32) NOT NULL COMMENT '角色编号',
    MODIFY COLUMN menu_id varchar(32) NOT NULL COMMENT '菜单编号',
    MODIFY COLUMN created_by varchar(32) NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除';

ALTER TABLE sys_org
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '部门编号',
    MODIFY COLUMN scope_id varchar(32) NOT NULL COMMENT '所属单位编号',
    MODIFY COLUMN parent_id varchar(32) NULL DEFAULT NULL COMMENT '父部门编号',
    MODIFY COLUMN ancestors varchar(500) NOT NULL DEFAULT '' COMMENT '祖级部门链路',
    MODIFY COLUMN name varchar(100) NOT NULL COMMENT '部门名称',
    MODIFY COLUMN code varchar(64) NULL COMMENT '部门编码',
    MODIFY COLUMN leader_id varchar(32) NULL COMMENT '负责人编号',
    MODIFY COLUMN phone varchar(20) NULL COMMENT '联系电话',
    MODIFY COLUMN email varchar(100) NULL COMMENT '电子邮箱',
    MODIFY COLUMN sort int NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN status tinyint NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    MODIFY COLUMN created_by varchar(32) NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号';

ALTER TABLE sys_data_audit
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '审计记录编号',
    MODIFY COLUMN actor_id varchar(32) NOT NULL COMMENT '操作人编号',
    MODIFY COLUMN scope_id varchar(32) NOT NULL COMMENT '操作单位编号',
    MODIFY COLUMN resource varchar(64) NOT NULL COMMENT '资源名称',
    MODIFY COLUMN record_id varchar(32) NOT NULL COMMENT '业务记录编号',
    MODIFY COLUMN operation varchar(20) NOT NULL COMMENT '操作类型',
    MODIFY COLUMN authorized_view varchar(32) NOT NULL COMMENT '授权视图',
    MODIFY COLUMN outcome varchar(20) NOT NULL COMMENT '操作结果',
    MODIFY COLUMN occurred_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间';

ALTER TABLE sys_configure
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '配置编号',
    MODIFY COLUMN name varchar(50) NOT NULL COMMENT '配置名称',
    MODIFY COLUMN `value` varchar(100) NULL COMMENT '配置值',
    MODIFY COLUMN type tinyint NOT NULL DEFAULT 1 COMMENT '配置类型：1系统参数，2第三方参数',
    MODIFY COLUMN desp varchar(255) NULL COMMENT '配置说明',
    MODIFY COLUMN sort int NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    MODIFY COLUMN created_by varchar(32) NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE param_basic
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '字典分类编号',
    MODIFY COLUMN name varchar(20) NOT NULL COMMENT '字典分类名称',
    MODIFY COLUMN system_name varchar(255) NOT NULL COMMENT '字典内部名称',
    MODIFY COLUMN sort int NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    MODIFY COLUMN created_by varchar(32) NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    MODIFY COLUMN system_builtin tinyint NOT NULL DEFAULT 0 COMMENT '系统内置标记：1只读，0业务可维护';

ALTER TABLE param_basic_type
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '字典项编号',
    MODIFY COLUMN basic_id varchar(32) NOT NULL COMMENT '字典分类编号',
    MODIFY COLUMN param_key varchar(20) NOT NULL COMMENT '字典项编码',
    MODIFY COLUMN param_value varchar(20) NOT NULL COMMENT '字典项值',
    MODIFY COLUMN desp varchar(200) NULL COMMENT '字典项描述',
    MODIFY COLUMN sort int NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    MODIFY COLUMN created_by varchar(32) NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE sys_login_audit
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '登录审计编号',
    MODIFY COLUMN login_name varchar(128) NOT NULL COMMENT '登录名',
    MODIFY COLUMN user_id varchar(32) NULL COMMENT '用户编号',
    MODIFY COLUMN scope_id varchar(32) NULL COMMENT '核验单位编号',
    MODIFY COLUMN client_ip varchar(64) NULL COMMENT '客户端IP',
    MODIFY COLUMN outcome varchar(20) NOT NULL COMMENT '认证结果',
    MODIFY COLUMN reason varchar(64) NULL COMMENT '结果原因',
    MODIFY COLUMN occurred_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发生时间';

ALTER TABLE sys_file_entry
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '文件编号',
    MODIFY COLUMN scope_id varchar(32) NOT NULL COMMENT '所属单位编号',
    MODIFY COLUMN org_id varchar(32) NULL COMMENT '所属部门编号',
    MODIFY COLUMN name varchar(200) NOT NULL COMMENT '文件名称',
    MODIFY COLUMN storage_profile varchar(64) NOT NULL COMMENT '存储配置标识',
    MODIFY COLUMN object_key varchar(32) NOT NULL COMMENT '存储对象键',
    MODIFY COLUMN byte_size bigint NULL COMMENT '文件大小（字节）',
    MODIFY COLUMN sha256 char(64) NULL COMMENT '文件SHA-256校验值',
    MODIFY COLUMN state varchar(16) NOT NULL COMMENT '文件状态',
    MODIFY COLUMN expected_bytes bigint NULL COMMENT '预期文件大小（字节）',
    MODIFY COLUMN part_bytes int NULL COMMENT '分片大小（字节）',
    MODIFY COLUMN expires_at datetime NULL COMMENT '上传过期时间',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号',
    MODIFY COLUMN created_by varchar(32) NOT NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NOT NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL COMMENT '更新时间',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    MODIFY COLUMN dir_id varchar(32) NULL COMMENT '文件目录编号',
    MODIFY COLUMN temporary_cleaned_at datetime NULL COMMENT '临时对象清理完成时间',
    MODIFY COLUMN cleanup_retry_at datetime NULL COMMENT '清理重试时间';

ALTER TABLE sys_file_part
    MODIFY COLUMN file_id varchar(32) NOT NULL COMMENT '文件编号',
    MODIFY COLUMN scope_id varchar(32) NOT NULL COMMENT '所属单位编号',
    MODIFY COLUMN actor_id varchar(32) NOT NULL COMMENT '上传操作人编号',
    MODIFY COLUMN part_index int NOT NULL COMMENT '分片序号',
    MODIFY COLUMN object_key varchar(32) NOT NULL COMMENT '分片对象键',
    MODIFY COLUMN expected_sha256 char(64) NOT NULL COMMENT '分片SHA-256校验值',
    MODIFY COLUMN ready tinyint NOT NULL DEFAULT 0 COMMENT '分片就绪标记：0未就绪，1已就绪';

ALTER TABLE sys_file_dir
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '目录编号',
    MODIFY COLUMN scope_id varchar(32) NOT NULL COMMENT '所属单位编号',
    MODIFY COLUMN org_id varchar(32) NULL COMMENT '所属部门编号',
    MODIFY COLUMN name varchar(100) NOT NULL COMMENT '目录名称',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号',
    MODIFY COLUMN created_by varchar(32) NOT NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NOT NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL COMMENT '更新时间',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除';

ALTER TABLE sys_org_notice
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '通知编号',
    MODIFY COLUMN scope_id varchar(32) NOT NULL COMMENT '所属单位编号',
    MODIFY COLUMN title varchar(100) NOT NULL COMMENT '通知标题',
    MODIFY COLUMN type varchar(64) NULL COMMENT '通知类型编码',
    MODIFY COLUMN type_label varchar(200) NULL COMMENT '通知类型名称',
    MODIFY COLUMN content varchar(2000) NOT NULL COMMENT '通知正文',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号',
    MODIFY COLUMN created_by varchar(32) NOT NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NOT NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL COMMENT '更新时间',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除';

ALTER TABLE sys_org_notice_ref
    MODIFY COLUMN scope_id varchar(32) NOT NULL COMMENT '所属单位编号',
    MODIFY COLUMN notice_id varchar(32) NOT NULL COMMENT '通知编号',
    MODIFY COLUMN user_id varchar(32) NOT NULL COMMENT '阅读用户编号',
    MODIFY COLUMN first_read_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次阅读时间';

ALTER TABLE sys_file_binding
    MODIFY COLUMN scope_id varchar(32) NOT NULL COMMENT '业务范围单位编号',
    MODIFY COLUMN resource varchar(32) NOT NULL COMMENT '关联资源类型',
    MODIFY COLUMN record_id varchar(32) NOT NULL COMMENT '业务记录编号',
    MODIFY COLUMN file_id varchar(32) NOT NULL COMMENT '文件编号',
    MODIFY COLUMN sort int NOT NULL COMMENT '附件排序号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
    MODIFY COLUMN file_scope_id varchar(32) NOT NULL COMMENT '文件所属单位编号';

ALTER TABLE sys_notice
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '通知编号',
    MODIFY COLUMN title varchar(100) NOT NULL COMMENT '通知标题',
    MODIFY COLUMN type varchar(20) NOT NULL COMMENT '通知类型编码',
    MODIFY COLUMN type_label varchar(20) NOT NULL COMMENT '通知类型名称',
    MODIFY COLUMN content varchar(2000) NOT NULL COMMENT '通知正文',
    MODIFY COLUMN is_show boolean NOT NULL DEFAULT FALSE COMMENT '是否显示：0否，1是',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    MODIFY COLUMN created_by varchar(32) NOT NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NOT NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE sys_version
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '版本记录编号',
    MODIFY COLUMN release_number varchar(10) NOT NULL COMMENT '发布版本号',
    MODIFY COLUMN date_update date NOT NULL COMMENT '发布日期',
    MODIFY COLUMN content varchar(1000) NOT NULL COMMENT '更新内容',
    MODIFY COLUMN type varchar(20) NOT NULL COMMENT '版本类型编码',
    MODIFY COLUMN type_label varchar(20) NOT NULL COMMENT '版本类型名称',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    MODIFY COLUMN created_by varchar(32) NOT NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NOT NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE param_region
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '地区编号',
    MODIFY COLUMN parent_id varchar(32) NULL COMMENT '上级地区编号',
    MODIFY COLUMN city_code varchar(20) NOT NULL COMMENT '地区编码',
    MODIFY COLUMN name varchar(20) NOT NULL COMMENT '地区名称',
    MODIFY COLUMN short_name varchar(20) NULL COMMENT '地区简称',
    MODIFY COLUMN zip_code varchar(6) NULL COMMENT '邮政编码',
    MODIFY COLUMN longitude decimal(18,15) NULL COMMENT '经度',
    MODIFY COLUMN latitude decimal(17,15) NULL COMMENT '纬度',
    MODIFY COLUMN level int NOT NULL COMMENT '地区层级',
    MODIFY COLUMN sort int NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    MODIFY COLUMN created_by varchar(32) NOT NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NOT NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE sys_serial_rule
    MODIFY COLUMN scope_id varchar(32) NOT NULL COMMENT '所属单位编号',
    MODIFY COLUMN business_code varchar(32) NOT NULL COMMENT '业务编码',
    MODIFY COLUMN prefix varchar(32) NOT NULL COMMENT '号段前缀',
    MODIFY COLUMN period_type varchar(8) NOT NULL COMMENT '周期类型',
    MODIFY COLUMN width int NOT NULL COMMENT '序号位宽',
    MODIFY COLUMN zone_id varchar(64) NOT NULL COMMENT '时区标识',
    MODIFY COLUMN permission varchar(100) NOT NULL COMMENT '使用权限',
    MODIFY COLUMN created_by varchar(32) NOT NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';

ALTER TABLE sys_serial_counter
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '计数器编号',
    MODIFY COLUMN scope_id varchar(32) NOT NULL COMMENT '所属单位编号',
    MODIFY COLUMN business_code varchar(32) NOT NULL COMMENT '业务编码',
    MODIFY COLUMN period_key varchar(16) NOT NULL COMMENT '周期键',
    MODIFY COLUMN sequence_value bigint NOT NULL DEFAULT 0 COMMENT '当前序号值',
    MODIFY COLUMN updated_by varchar(32) NOT NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';

ALTER TABLE sys_user_scope_org
    MODIFY COLUMN member_id varchar(32) NOT NULL COMMENT '用户资料编号',
    MODIFY COLUMN scope_id varchar(32) NOT NULL COMMENT '所属单位编号',
    MODIFY COLUMN org_id varchar(32) NOT NULL COMMENT '部门编号';

ALTER TABLE sys_recycle_resource
    MODIFY COLUMN id varchar(32) NOT NULL COMMENT '回收资源编号',
    MODIFY COLUMN code varchar(50) NOT NULL COMMENT '资源编码',
    MODIFY COLUMN name varchar(50) NOT NULL COMMENT '资源名称',
    MODIFY COLUMN table_name varchar(64) NOT NULL COMMENT '业务表名',
    MODIFY COLUMN title_column varchar(64) NOT NULL COMMENT '标题字段名',
    MODIFY COLUMN search_column varchar(64) NOT NULL COMMENT '查询字段名',
    MODIFY COLUMN scope_required tinyint NOT NULL DEFAULT 0 COMMENT '是否需要单位范围：0否，1是',
    MODIFY COLUMN scope_column varchar(64) NULL COMMENT '单位字段名',
    MODIFY COLUMN department_column varchar(64) NULL COMMENT '部门字段名',
    MODIFY COLUMN ancestors_column varchar(64) NULL COMMENT '祖级字段名',
    MODIFY COLUMN actor_column varchar(64) NULL COMMENT '操作人字段名',
    MODIFY COLUMN version_column varchar(64) NOT NULL COMMENT '版本字段名',
    MODIFY COLUMN deleted_column varchar(64) NOT NULL COMMENT '删除标记字段名',
    MODIFY COLUMN created_at_column varchar(64) NOT NULL COMMENT '创建时间字段名',
    MODIFY COLUMN updated_at_column varchar(64) NOT NULL COMMENT '更新时间字段名',
    MODIFY COLUMN updated_by_column varchar(64) NOT NULL COMMENT '更新人字段名',
    MODIFY COLUMN sort int NOT NULL DEFAULT 0 COMMENT '排序号',
    MODIFY COLUMN version int NOT NULL DEFAULT 0 COMMENT '版本号',
    MODIFY COLUMN enabled tinyint NOT NULL DEFAULT 1 COMMENT '是否启用：0否，1是',
    MODIFY COLUMN deleted tinyint NOT NULL DEFAULT 0 COMMENT '删除标记：0未删除，1已删除',
    MODIFY COLUMN created_by varchar(32) NOT NULL COMMENT '创建人编号',
    MODIFY COLUMN created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN updated_by varchar(32) NOT NULL COMMENT '更新人编号',
    MODIFY COLUMN updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
