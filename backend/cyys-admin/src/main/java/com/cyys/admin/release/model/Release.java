package com.cyys.admin.release.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("sys_version")
public class Release {
    private String id;
    private String releaseNumber;
    private LocalDate dateUpdate;
    private String type;
    private String typeLabel;
    private String content;
    private Integer version = 0;
    private LocalDateTime createdAt;
}
