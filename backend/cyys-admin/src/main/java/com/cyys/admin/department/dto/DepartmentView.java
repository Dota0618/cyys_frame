package com.cyys.admin.department.dto;

import com.cyys.admin.department.model.SysOrg;

public record DepartmentView(String id, String scopeId, String parentId, String name, String code,
                   String leaderId, String leaderName, boolean leaderActive, int version) {
    public static DepartmentView from(SysOrg org, DepartmentLeaderOption leader) {
        String leaderName = leader == null && org.getLeaderId() != null
                ? "已失效成员（" + org.getLeaderId() + "）"
                : leader == null ? null : leader.label();
        return new DepartmentView(org.getId(), org.getScopeId(), org.getParentId(), org.getName(), org.getCode(),
                org.getLeaderId(), leaderName, leader != null && !leader.disabled(), org.getVersion());
    }
}
