package @@PACKAGE@@.model;

import com.baomidou.mybatisplus.annotation.*;
import com.cyys.common.mybatis.DataResourceDefinition;
import java.math.BigDecimal;
import java.time.*;

@TableName("@@TABLE@@")
@DataResourceDefinition(name="@@FEATURE@@",scopeColumn="scope_id",departmentColumn="org_id",actorColumn="created_by",versionColumn="version",immutableColumns={@@IMMUTABLE@@})
public class @@CLASS@@ {
    @TableId(type=IdType.ASSIGN_ID) private String id;
    @TableField(fill=FieldFill.INSERT,updateStrategy=FieldStrategy.NEVER) private String scopeId;
    @TableField(updateStrategy=FieldStrategy.NEVER) private String orgId;
    @Version private Integer version=0;
    @TableLogic private Integer deleted=0;
    @TableField(fill=FieldFill.INSERT,updateStrategy=FieldStrategy.NEVER) private String createdBy;
    @TableField(fill=FieldFill.INSERT,updateStrategy=FieldStrategy.NEVER) private LocalDateTime createdAt;
    @TableField(fill=FieldFill.INSERT_UPDATE) private String updatedBy;
    @TableField(fill=FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    public String getId() { return id; }
    public void setId(String value) { id=value; }
    public String getScopeId() { return scopeId; }
    public void setScopeId(String value) { scopeId=value; }
    public String getOrgId() { return orgId; }
    public void setOrgId(String value) { orgId=value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { version=value; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer value) { deleted=value; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String value) { createdBy=value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt=value; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String value) { updatedBy=value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt=value; }
    @@PARENT_FIELD@@
    @@MODEL_FIELDS@@
}
