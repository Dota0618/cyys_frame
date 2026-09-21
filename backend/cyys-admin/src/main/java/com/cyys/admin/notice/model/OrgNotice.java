package com.cyys.admin.notice.model;

import com.baomidou.mybatisplus.annotation.*;
import com.cyys.common.mybatis.DataResourceDefinition;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_org_notice")
@DataResourceDefinition(name="org_notice",immutableColumns={"org_id"})
public class OrgNotice {
    @TableId(type=IdType.ASSIGN_ID) private String id;
    @TableField(fill=FieldFill.INSERT,updateStrategy=FieldStrategy.NEVER) private String scopeId;
    @TableField(updateStrategy=FieldStrategy.NEVER) private String orgId;
    private String title;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String type;
    @TableField(updateStrategy=FieldStrategy.ALWAYS) private String typeLabel;
    private String content;
    @Version private Integer version=0;
    @TableField(fill=FieldFill.INSERT,updateStrategy=FieldStrategy.NEVER) private String createdBy;
    @TableField(fill=FieldFill.INSERT,updateStrategy=FieldStrategy.NEVER) private LocalDateTime createdAt;
    @TableField(fill=FieldFill.INSERT_UPDATE) private String updatedBy;
    @TableField(fill=FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
    @TableLogic private Integer deleted=0;
}
