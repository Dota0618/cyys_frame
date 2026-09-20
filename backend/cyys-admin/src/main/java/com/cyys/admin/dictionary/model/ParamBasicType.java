package com.cyys.admin.dictionary.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("param_basic_type")
public class ParamBasicType {
    private String id;
    private String basicId;
    private String paramKey;
    private String paramValue;
    private String desp;
    private Integer sort;
    private Integer version = 0;
}
