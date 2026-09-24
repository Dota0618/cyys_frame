package com.cyys.admin.recycle.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 后台登记的受控软删除资源；表名和列名只来自服务端校验过的数据库元数据。 */
@Data
@TableName("sys_recycle_resource")
public class RecycleConfiguration {
    private String id;
    private String code;
    private String name;
    private String tableName;
    private String titleColumn;
    private String searchColumn;
    private Integer scopeRequired;
    private String scopeColumn;
    private String departmentColumn;
    private String ancestorsColumn;
    private String actorColumn;
    private String versionColumn;
    private String deletedColumn;
    private String createdAtColumn;
    private String updatedAtColumn;
    private String updatedByColumn;
    private Integer sort;
    private Integer version = 0;
    private Integer enabled = 1;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
