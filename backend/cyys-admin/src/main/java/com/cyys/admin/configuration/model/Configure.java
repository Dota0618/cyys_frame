package com.cyys.admin.configuration.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_configure")
public class Configure {
    private String id;
    private String name;
    private String value;
    private Integer type;
    private String desp;
    private Integer sort;
    private Integer version = 0;
}
