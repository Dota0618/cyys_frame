package com.cyys.admin.region.dto;

import com.cyys.admin.region.model.Region;
import java.math.BigDecimal;

public record RegionView(String id,String parentId,String cityCode,String name,String shortName,String zipCode,
                         BigDecimal longitude,BigDecimal latitude,int level,int sort,int version,boolean hasChildren) {
    public static RegionView from(Region row) {
        return new RegionView(row.getId(),row.getParentId(),row.getCityCode(),row.getName(),row.getShortName(),
                row.getZipCode(),row.getLongitude(),row.getLatitude(),row.getLevel(),row.getSort(),row.getVersion(),row.isHasChildren());
    }
}
