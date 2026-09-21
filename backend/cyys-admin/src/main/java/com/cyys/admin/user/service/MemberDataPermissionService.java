package com.cyys.admin.user.service;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.UnitAdministrationService;
import com.cyys.admin.user.dto.*;
import com.cyys.admin.user.mapper.MemberDataPermissionMapper;
import com.cyys.admin.user.model.SysUserScope;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberDataPermissionService {
    private final MemberDataPermissionMapper mapper;
    private final UnitAdministrationService administration;
    private final DataAuditService audit;
    private final com.cyys.admin.auth.service.PlatformSecurityService security;
    private static final String PERMISSION = "user:data-permission";

    @Transactional(readOnly = true)
    public MemberDataPermissionView detail(String id) {
        administration.authorize(PERMISSION, false);
        String scope = ScopeContext.get().requireScopeId();
        var member = requireMember(mapper.detail(id, scope));
        administration.requireAssignableRange(member, member.getDataRange());
        return view(member, member.getScopeId());
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MemberDataPermissionView update(String id, UpdateMemberDataPermissionCommand command) {
        var identity = ScopeContext.get();
        var member = lockForUpdate(id, command.version());
        String scope = member.getScopeId();
        administration.requireAssignableRange(member, command.dataRange());
        if (new HashSet<>(command.orgIds()).size() != command.orgIds().size()) throw new ApiException(400, "部门不能重复");
        if (command.dataRange() == 2) {
            if (command.orgIds().isEmpty()) throw new ApiException(400, "自定义范围至少选择一个部门");
            if (mapper.lockDepartments(command.orgIds(), scope).size() != command.orgIds().size())
                throw new ApiException(403, "不能选择其他单位或无效部门");
        } else if (!command.orgIds().isEmpty()) throw new ApiException(400, "仅自定义范围可以选择部门");
        if (mapper.update(id, scope, command.version(), command.dataRange(), identity.loginId()) != 1)
            throw new ApiException(409, "成员已变更，请刷新后重试");
        mapper.removeDepartments(id, scope);
        for (String org : command.orgIds()) {
            if (mapper.addDepartment(id, scope, org) != 1) throw new ApiException(409, "数据权限保存冲突");
        }
        security.requireUsableAdministrator();
        audit.success("user-data-permission", id, "UPDATE");
        member.setDataRange(command.dataRange()); member.setVersion(command.version() + 1);
        return view(member, scope);
    }

    // Called inside the same transaction before basic edits, preserving lock order.
    SysUserScope lockForUpdate(String id, Integer version) {
        var identity = ScopeContext.get();
        String scope = identity.requireScopeId();
        security.lockAdministratorGuard();
        // 先检查操作权；锁定后实时核验操作者与目标的数据范围。
        administration.authorize(PERMISSION, false);
        var member = requireMember(mapper.lockMembers(id, scope, identity.loginId()).stream()
                .filter(row -> row.getId().equals(id)).findFirst().orElse(null));
        administration.authorize(PERMISSION, false);
        if (member.getScopeId() != null && !scope.equals(member.getScopeId())) throw new ApiException(403, "目标用户不属于所选机构");
        administration.requireAssignableRange(member, member.getDataRange());
        if (!Objects.equals(member.getVersion(), version)) throw new ApiException(409, "成员已变更，请刷新后重试");
        return member;
    }

    private MemberDataPermissionView view(SysUserScope member, String scope) {
        return new MemberDataPermissionView(member.getId(), member.getVersion(), member.getDataRange(),
                mapper.orgIds(member.getId(), scope), mapper.departments(scope));
    }
    private static SysUserScope requireMember(SysUserScope row) {
        if (row == null) throw new ApiException(404, "用户资料不存在");
        return row;
    }
}
