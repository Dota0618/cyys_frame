package com.cyys.admin.region.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("param_region")
public class Region {
    private String id;
    private String parentId;
    private String cityCode;
    private String name;
    private String shortName;
    private String zipCode;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer level;
    private Integer sort;
    private Integer version = 0;
    private boolean hasChildren;
}
