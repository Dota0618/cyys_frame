package com.cyys.admin.role.dto;

import com.cyys.admin.role.model.SysRole;

public record RoleView(String id, String scopeId, String code, String name,
                   int status, int sort, int version, String remark) {
    public static RoleView from(SysRole role) {
        return new RoleView(role.getId(), role.getScopeId(), role.getCode(), role.getName(),
                role.getStatus(), role.getSort(), role.getVersion(), role.getRemark());
    }
}
