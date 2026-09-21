package com.cyys.admin.user.service;

import com.cyys.admin.auth.service.PasswordPolicy;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.role.service.RoleGrantService;
import com.cyys.admin.user.dto.MemberQuery;
import com.cyys.admin.user.dto.ExportMembersCommand;
import com.cyys.admin.user.dto.CreateMemberCommand;
import com.cyys.admin.user.dto.UpdateMemberCommand;
import com.cyys.admin.user.dto.MemberView;
import com.cyys.admin.user.dto.MemberDetail;
import com.cyys.admin.user.dto.MemberOption;
import com.cyys.admin.user.dto.MemberDepartmentOption;
import com.cyys.admin.user.dto.MemberDataPermissionInput;
import com.cyys.admin.user.dto.UpdateMemberDataPermissionCommand;
import com.cyys.admin.user.mapper.MemberAccountMapper;
import com.cyys.admin.user.mapper.MemberMapper;
import com.cyys.admin.user.mapper.MemberDepartmentMapper;
import com.cyys.admin.user.model.SysUserScope;
import com.cyys.common.mybatis.*;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.export.BoundedXlsx;
import com.cyys.common.web.ApiException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.Map;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor
public class MemberService {
    private final MemberMapper mapper;
    private final MemberAccountMapper accounts;
    private final MemberDepartmentMapper departments;
    private final RowPolicyProvider policy;
    private final DataPolicyRegistry registry;
    private final RoleGrantService grants;
    private final DataAuditService audit;
    private final UserDisplayService cache;
    private final MemberDataPermissionService dataPermissions;
    private final com.cyys.admin.auth.service.PlatformSecurityService security;

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public byte[] export(@Valid ExportMembersCommand command) {
        authorize("user:export", DataPolicyRegistry.Access.WRITE);
        String name = command.name() == null || command.name().isBlank() ? null : command.name();
        var members = mapper.export(name, BoundedXlsx.MAX_ROWS + 1);
        if (members.size() > BoundedXlsx.MAX_ROWS) throw new ApiException(422, "最多导出 5000 条，请缩小姓名筛选范围");
        var logins = names(members);
        var departmentIds = members.stream().map(SysUserScope::getOrgId).filter(Objects::nonNull).distinct().toList();
        var visibleDepartments = departmentIds.isEmpty() ? List.<MemberDepartmentOption>of() : departments.exportOptions(departmentIds);
        var departmentNames = visibleDepartments.stream().collect(Collectors.toMap(MemberDepartmentOption::value, MemberDepartmentOption::label));
        byte[] result;
        try {
            result = BoundedXlsx.write("单位成员", List.of("成员姓名", "登录账号", "部门", "成员状态", "创建时间"),
                    members.stream().map(member -> {
                        String login = logins.get(member.getUserId());
                        return Arrays.asList(member.getDisplayName() == null ? login : member.getDisplayName(), login,
                                member.getOrgId() == null ? "未设置" : departmentNames.getOrDefault(member.getOrgId(), "不可见或已失效"),
                                member.getStatus() == 1 ? "启用" : "停用",
                                member.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    }).toList());
        } catch (IOException failure) { throw new ApiException(503, "导出文件生成失败，请稍后重试"); }
        // 权限仍存在不代表范围仍相同；重新通过受保护查询检查每个实际导出的成员和部门。
        authorize("user:export", DataPolicyRegistry.Access.WRITE);
        if (!members.isEmpty() && mapper.selectCount(new QueryWrapper<SysUserScope>().in("id", members.stream().map(SysUserScope::getId).toList())) != members.size()) {
            throw new ApiException(409, "成员或导出范围已变化，请重新导出");
        }
        if (!visibleDepartments.isEmpty() && departments.exportOptions(visibleDepartments.stream().map(MemberDepartmentOption::value).toList()).size() != visibleDepartments.size()) {
            throw new ApiException(409, "部门可见范围已变化，请重新导出");
        }
        audit.success("user", IdWorker.getIdStr(), "EXPORT_GENERATED");
        return result;
    }

    @Transactional(readOnly=true)
    public IPage<MemberView> page(@Valid MemberQuery query) {
        var page = new Page<SysUserScope>(query.page(), query.size());
        page.setOptimizeCountSql(false);
        var filter = new QueryWrapper<SysUserScope>().like(query.name()!=null&&!query.name().isBlank(),"display_name",query.name())
                .orderByDesc("created_at").orderByAsc("id");
        return views(mapper.selectPage(page, filter));
    }

    @Transactional(readOnly=true)
    public IPage<MemberView> all(@Valid MemberQuery query) {
        var page = new Page<SysUserScope>(query.page(),query.size());
        page.setOptimizeCountSql(false);
        page.setRecords(mapper.acrossScopes(page));
        return views(page);
    }

    @Transactional(readOnly=true)
    public MemberDetail detail(String id) {
        var member=mapper.selectById(id);
        if(member==null) throw new ApiException(404,"成员不存在或不可访问");
        return new MemberDetail(view(member,names(List.of(member))),accounts.roleIds(member.getUserId()));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MemberView create(@Valid CreateMemberCommand command) {
        authorize("user:create",DataPolicyRegistry.Access.WRITE);
        String scope=ScopeContext.get().requireScopeId();
        PasswordPolicy.validateNew(command.password());
        if(command.orgId()!=null&&departments.forCreate(command.orgId())==null) throw new ApiException(403,"关联部门不可用");
        var member=new SysUserScope();
        member.setId(IdWorker.getIdStr()); member.setUserId(IdWorker.getIdStr()); member.setScopeId(scope);
        member.setDisplayName(command.displayName()); member.setOrgId(command.orgId()); member.setStatus(1);
        try {
            accounts.createAccount(member.getUserId(),command.loginName(),PasswordPolicy.hash(command.password()),ScopeContext.get().loginId());
            if(mapper.insert(member)!=1) throw new ApiException(409,"新增成员失败");
        } catch(DataIntegrityViolationException error) { throw new ApiException(409,"登录账号不可使用"); }
        saveDataPermission(member, command.dataPermission());
        audit.success("user",member.getId(),"CREATE");
        return view(member,Map.of(member.getUserId(),command.loginName()));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MemberView edit(String id,@Valid UpdateMemberCommand command) {
        security.lockAdministratorGuard();
        if (command.dataPermission() != null) dataPermissions.lockForUpdate(id, command.version());
        var member=mapper.lockForEdit(id);
        if(member==null||!member.getVersion().equals(command.version())) throw new ApiException(409,"成员不可修改或已变更");
        security.requireDataRangeCeiling(member.getDataRange());
        if(command.orgId()!=null&&departments.forEdit(command.orgId())==null) throw new ApiException(403,"关联部门不可用");
        if (member.getScopeId() == null && command.orgId() != null) throw new ApiException(400,"系统账号尚未设置所属机构，不能关联部门");
        member.setDisplayName(command.displayName()); member.setOrgId(command.orgId()); member.setStatus(command.status());
        if(mapper.updateById(member)!=1) throw new ApiException(409,"成员已变更");
        saveDataPermission(member, command.dataPermission());
        security.requireUsableAdministrator();
        audit.success("user",id,"UPDATE");
        cache.invalidateAfterCommit();
        return view(member,names(List.of(member)));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void grantRoles(String id,List<String> roles) {
        security.lockAdministratorGuard();
        var member=mapper.lockForGrant(id);
        if(member==null) throw new ApiException(403,"成员不可操作");
        grants.replaceRoles(member.getUserId(),roles);
    }

    public List<MemberDepartmentOption> departments() {
        authorize("user:read",DataPolicyRegistry.Access.READ);
        return departments.options();
    }

    public List<MemberOption> roles() {
        authorize("role:grant",DataPolicyRegistry.Access.WRITE);
        return grants.grantableRoleOptions().stream().map(role -> new MemberOption(role.value(), role.label())).toList();
    }

    private void authorize(String permission,DataPolicyRegistry.Access access) {
        var identity=ScopeContext.get();
        DataTransactionGuard.bind(identity,DataPolicyRegistry.View.CURRENT_SCOPE);
        policy.authorize(new DataPolicyRegistry.Policy(registry.resource(SysUserScope.class),permission,access,DataPolicyRegistry.View.CURRENT_SCOPE),identity);
    }
    private void saveDataPermission(SysUserScope member, MemberDataPermissionInput input) {
        if (input == null) return;
        var saved = dataPermissions.update(member.getId(), new UpdateMemberDataPermissionCommand(
                member.getVersion(), input.dataRange(), input.orgIds()));
        member.setVersion(saved.version());
        member.setDataRange(saved.dataRange());
    }
    private IPage<MemberView> views(IPage<SysUserScope> page) {
        Map<String,String> names=names(page.getRecords());
        return page.convert(member->view(member,names));
    }
    private Map<String,String> names(List<SysUserScope> members) {
        if(members.isEmpty()) return Map.of();
        return accounts.accountNames(members.stream().map(SysUserScope::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(MemberOption::value,MemberOption::label));
    }
    private MemberView view(SysUserScope member,Map<String,String> names) {
        String login=names.get(member.getUserId());
        return new MemberView(member.getId(),member.getUserId(),member.getScopeId(),login,
                member.getDisplayName()==null?login:member.getDisplayName(),member.getOrgId(),member.getStatus(),member.getVersion(),member.getCreatedAt(),member.getDataRange());
    }
}
