package com.cyys.admin.user.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
/**
 * Global login account.
 * 全局登录账号。
 */
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String loginName;

    /** BCrypt password hash, never store plaintext or legacy MD5 here. / BCrypt 密码散列，不允许存明文或旧 MD5。 */
    private String passwordHash;

    private String nickname;

    private String realName;

    private String avatar;

    private String phone;

    private String email;

    private String idCard;

    private Integer status;

    /** Consecutive password failure count for lightweight login protection. / 轻量登录保护用的连续密码错误次数。 */
    private Integer pwdErrorCount;

    /** Lock expiry time after too many password failures. / 密码连续输错后的锁定截止时间。 */
    private LocalDateTime pwdLockTime;

    private LocalDateTime pwdLastChanged;

    private LocalDateTime lastLoginTime;

    private String lastLoginIp;

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
