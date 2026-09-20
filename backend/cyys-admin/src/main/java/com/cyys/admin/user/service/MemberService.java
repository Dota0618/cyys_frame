package com.cyys.admin.user.service;

import com.cyys.admin.auth.service.PasswordPolicy;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.role.service.RoleGrantService;
import com.cyys.admin.user.dto.MemberQuery;
import com.cyys.admin.user.dto.CreateMemberCommand;
import com.cyys.admin.user.dto.UpdateMemberCommand;
import com.cyys.admin.user.dto.MemberView;
import com.cyys.admin.user.dto.MemberDetail;
import com.cyys.admin.user.dto.MemberOption;
import com.cyys.admin.user.dto.MemberDepartmentOption;
import com.cyys.admin.user.mapper.MemberAccountMapper;
import com.cyys.admin.user.mapper.MemberMapper;
import com.cyys.admin.user.mapper.MemberDepartmentMapper;
import com.cyys.admin.user.model.SysUserScope;
import com.cyys.common.mybatis.*;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.Map;
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
        return new MemberDetail(view(member,names(List.of(member))),accounts.roleIds(member.getUserId(),member.getScopeId()));
    }

    @Transactional
    public MemberView create(@Valid CreateMemberCommand command) {
        authorize("user:create",DataPolicyRegistry.Access.WRITE);
        String scope=ScopeContext.get().requireScopeId();
        PasswordPolicy.validateNew(command.password());
        if(command.orgId()!=null&&departments.forCreate(command.orgId())==null) throw new ApiException(403,"关联部门不可用");
        var member=new SysUserScope();
        member.setId(IdWorker.getIdStr()); member.setUserId(IdWorker.getIdStr()); member.setScopeId(scope);
        member.setDisplayName(command.displayName()); member.setOrgId(command.orgId()); member.setStatus(1);
        member.setMemberRole("MEMBER"); member.setDefaultFlag(1);
        try {
            accounts.createAccount(member.getUserId(),command.loginName(),PasswordPolicy.hash(command.password()),ScopeContext.get().loginId());
            if(mapper.insert(member)!=1) throw new ApiException(409,"新增成员失败");
        } catch(DataIntegrityViolationException error) { throw new ApiException(409,"登录账号不可使用"); }
        audit.success("user",member.getId(),"CREATE");
        return view(member,Map.of(member.getUserId(),command.loginName()));
    }

    @Transactional
    public MemberView edit(String id,@Valid UpdateMemberCommand command) {
        var member=mapper.lockForEdit(id);
        if(member==null||!member.getVersion().equals(command.version())) throw new ApiException(409,"成员不可修改或已变更");
        if(command.orgId()!=null&&departments.forEdit(command.orgId())==null) throw new ApiException(403,"关联部门不可用");
        member.setDisplayName(command.displayName()); member.setOrgId(command.orgId()); member.setStatus(command.status());
        if(mapper.updateById(member)!=1) throw new ApiException(409,"成员已变更");
        audit.success("user",id,"UPDATE");
        cache.invalidateAfterCommit();
        return view(member,names(List.of(member)));
    }

    @Transactional
    public void grantRoles(String id,List<String> roles) {
        var member=mapper.lockForGrant(id);
        if(member==null) throw new ApiException(403,"成员不可操作");
        grants.replaceUnitRoles(member.getUserId(),roles);
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
                member.getDisplayName()==null?login:member.getDisplayName(),member.getOrgId(),member.getStatus(),member.getVersion(),member.getCreatedAt());
    }
}
