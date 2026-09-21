package com.cyys.admin.department.dto;

import com.cyys.admin.department.model.SysOrg;
import com.cyys.common.cache.CacheValue;

public record DepartmentView(String id, String scopeId, String parentId, String name, String code,
                   String leaderId,
                   @CacheValue(cache="user", key="leaderId", field="name") String leaderName,
                   @CacheValue(cache="user", key="leaderId", field="active") Boolean leaderActive, int version) {
    public static DepartmentView from(SysOrg org) {
        return new DepartmentView(org.getId(), org.getScopeId(), org.getParentId(), org.getName(), org.getCode(),
                org.getLeaderId(), null, null, org.getVersion());
    }
}
