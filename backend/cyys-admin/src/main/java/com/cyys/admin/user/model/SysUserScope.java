package com.cyys.admin.user.model;

import com.baomidou.mybatisplus.annotation.*;
import com.cyys.common.mybatis.DataResourceDefinition;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_user_scope")
@DataResourceDefinition(name = "user", scopeColumn = "scope_id",
        departmentColumn = "org_id", actorColumn = "user_id", versionColumn = "version",
        immutableColumns = {"user_id", "data_range"})
/**
 * One home institution and data range per user.
 * 用户唯一的所属机构、部门和数据范围。
 */
public class SysUserScope implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String userId;

    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    private String scopeId;

    private String displayName;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer dataRange = 5;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String orgId;

    @Version
    private Integer version = 0;


    private Integer status;


    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    private String createdBy;

    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
