package com.cyys.admin.menu.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_menu")
/**
 * Platform/shared menu definition.
 * 平台级/共享菜单定义。
 */
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String parentId;

    private String name;

    private String code;

    private String path;

    private String component;

    private String redirect;

    private String icon;

    /** 1=目录，2=菜单，3=按钮/操作权限，与数据库约束一致。 */
    private Integer type;

    private Integer sort;

    private Integer visible;

    private Integer status;

    /** Whether this menu is visible only to platform-level roles. / 是否仅平台角色可见。 */
    private Integer platformOnly;

    /** Frontend route cache hint for Ant Design Pro style pages. / 前端路由缓存提示。 */
    private Integer keepAlive;

    /** Whether to keep the root menu expanded even with a single child. / 即使只有一个子节点也保持根菜单展开。 */
    private Integer alwaysShow;

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
