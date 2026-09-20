package com.cyys.admin.dictionary.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("param_basic")
public class ParamBasic {
    private String id;
    private String name;
    private String systemName;
    private Integer sort;
    private Integer version = 0;
}
