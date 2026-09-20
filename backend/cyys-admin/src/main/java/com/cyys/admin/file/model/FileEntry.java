package com.cyys.admin.file.model;

import com.baomidou.mybatisplus.annotation.*;
import com.cyys.common.mybatis.DataResourceDefinition;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_file_entry")
@DataResourceDefinition(name = "file", scopeColumn = "scope_id", departmentColumn = "org_id",
        actorColumn = "created_by", versionColumn = "version",
        immutableColumns = {"org_id", "storage_profile", "object_key", "expected_bytes", "part_bytes", "expires_at"})
public class FileEntry {
    @TableId(type = IdType.ASSIGN_ID) private String id;
    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER) private String scopeId;
    @TableField(updateStrategy = FieldStrategy.NEVER) private String orgId;
    private String name;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String dirId;
    @TableField(updateStrategy = FieldStrategy.NEVER) private String storageProfile;
    @TableField(updateStrategy = FieldStrategy.NEVER) private String objectKey;
    private Long byteSize;
    private String sha256;
    private String state;
    @TableField(updateStrategy = FieldStrategy.NEVER) private Long expectedBytes;
    @TableField(updateStrategy = FieldStrategy.NEVER) private Integer partBytes;
    @TableField(updateStrategy = FieldStrategy.NEVER) private LocalDateTime expiresAt;
    @Version private Integer version = 0;
    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER) private String createdBy;
    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER) private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE) private String updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted = 0;
}
