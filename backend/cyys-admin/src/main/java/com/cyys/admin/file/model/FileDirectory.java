package com.cyys.admin.file.model;

import com.baomidou.mybatisplus.annotation.*;
import com.cyys.common.mybatis.DataResourceDefinition;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_file_dir")
@DataResourceDefinition(name="file_directory", scopeColumn="scope_id", departmentColumn="org_id",
        actorColumn="created_by", versionColumn="version", immutableColumns={"org_id"})
public class FileDirectory {
    @TableId(type=IdType.ASSIGN_ID) private String id;
    @TableField(fill=FieldFill.INSERT, updateStrategy=FieldStrategy.NEVER) private String scopeId;
    @TableField(updateStrategy=FieldStrategy.NEVER) private String orgId;
    private String name;
    @Version private Integer version=0;
    @TableField(fill=FieldFill.INSERT, updateStrategy=FieldStrategy.NEVER) private String createdBy;
    @TableField(fill=FieldFill.INSERT, updateStrategy=FieldStrategy.NEVER) private LocalDateTime createdAt;
    @TableField(fill=FieldFill.INSERT_UPDATE) private String updatedBy;
    @TableField(fill=FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted=0;
}
