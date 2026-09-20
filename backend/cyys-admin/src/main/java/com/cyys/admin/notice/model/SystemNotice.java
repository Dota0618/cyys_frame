package com.cyys.admin.notice.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_notice")
public class SystemNotice {
    private String id;
    private String title;
    private String type;
    private String typeLabel;
    private String content;
    private boolean visible;
    private Integer version = 0;
    private LocalDateTime createdAt;
}
