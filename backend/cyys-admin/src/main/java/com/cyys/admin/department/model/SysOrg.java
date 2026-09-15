package com.cyys.admin.department.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_org")
/**
 * Organization tree inside one scope.
 * 单个业务单位内的组织树。
 */
public class SysOrg implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    /** Owning scope/unit id. / 所属业务单位 ID。 */
    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    private String scopeId;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String parentId;

    /** Comma-separated ancestor id chain for subtree queries. / 逗号分隔的祖先链，便于子树查询。 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String ancestors;

    @Version
    private Integer version = 0;

    private String name;

    private String code;

    private String leaderId;

    private String phone;

    private String email;

    private Integer sort;

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
