package com.cyys.admin.scope.dto;

import com.cyys.admin.scope.model.SysScope;

public record ScopeView(String id,String code,String name,String shortName,String contactPerson,
                        String contactPhone,boolean enabled,int sort,String remark,int version) {
    public static ScopeView from(SysScope row) {
        return new ScopeView(row.getId(),row.getCode(),row.getName(),row.getShortName(),row.getContactPerson(),
                row.getContactPhone(),row.getStatus()==1,row.getSort(),row.getRemark(),row.getVersion());
    }
}
