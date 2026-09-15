package com.cyys.admin.role.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_role")
/**
 * Role definition.
 * 角色定义。
 */
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** GLOBAL for platform roles, concrete scope id for unit roles. / GLOBAL 表示平台角色，其他值表示单位角色。 */
    private String scopeId;

    private String code;

    private String name;

    /** 1 本单位全部，2 自定义部门，3 本部门，4 本部门及下级，5 本人；只合并持有所需操作的角色。 */
    private Integer dataRange;

    private Integer status;

    private Integer sort;

    @Version
    private Integer version = 0;

    private String remark;

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
