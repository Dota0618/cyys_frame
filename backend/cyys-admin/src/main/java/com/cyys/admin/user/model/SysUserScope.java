package com.cyys.admin.user.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_user_scope")
/**
 * Membership relation between a global account and a business scope.
 * 全局账号与业务单位之间的成员关系。
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

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String orgId;

    @Version
    private Integer version = 0;

    /** Member role inside the scope, such as OWNER or MEMBER. / 成员在单位内的角色标识，例如 OWNER 或 MEMBER。 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private String memberRole;

    /** Whether this scope is the user's default working scope. / 是否为用户默认工作单位。 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer defaultFlag;

    private Integer status;

    /** Optional membership expiry; null means long-term valid. / 可选的成员关系失效时间，null 表示长期有效。 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime expireAt;

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
