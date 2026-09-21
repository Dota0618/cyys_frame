package com.cyys.admin.role.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.role.dto.RoleQuery;
import com.cyys.admin.role.dto.CreateRoleCommand;
import com.cyys.admin.role.dto.UpdateRoleCommand;
import com.cyys.admin.role.dto.AssignRolesCommand;
import com.cyys.admin.role.dto.RoleView;
import com.cyys.admin.role.dto.RoleMenuOption;
import com.cyys.admin.role.dto.RoleOptions;
import com.cyys.admin.role.dto.RoleDetail;
import com.cyys.admin.role.dto.AccountRow;
import com.cyys.admin.role.dto.AccountView;
import com.cyys.admin.role.mapper.RoleMapper;
import com.cyys.admin.role.model.SysRole;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleMapper mapper;
    private final PlatformSecurityService security;
    private final DataAuditService audit;

    @Transactional(readOnly = true)
    public IPage<RoleView> page(RoleQuery query) {
        Page<SysRole> page = new Page<>(query.page(), query.size());
        page.setOptimizeCountSql(false);
        return mapper.page(page, query.search()).convert(RoleView::from);
    }

    @Transactional(readOnly = true)
    public RoleDetail detail(String id) {
        Set<String> ceiling = security.permissionCeiling();
        SysRole role = requireRole(mapper.detail(id));
        return new RoleDetail(RoleView.from(role), mapper.menuIds(id),
                withinCeiling(List.of(id), ceiling));
    }

    @Transactional(readOnly = true)
    public RoleOptions options() {
        Set<String> ceiling = security.permissionCeiling();
        return new RoleOptions(mapper.menus().stream()
                .filter(menu -> menu.type() != 3 || ceiling.contains(menu.code()))
                .toList());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public RoleView create(CreateRoleCommand command) {
        Set<String> ceiling = security.permissionCeiling();
        validateSelections(command.menuIds(), ceiling);
        var role = new SysRole();
        role.setId(IdWorker.getIdStr());
        role.setCode(command.code());
        apply(role, command.name(), command.status(), command.sort(), command.remark());
        try {
            requireOne(mapper.create(role, actor()));
            replaceMenus(role.getId(), command.menuIds());
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "角色编码或关联关系冲突");
        }
        audit.platformSuccess("role", role.getId(), "CREATE");
        return RoleView.from(role);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public RoleView edit(UpdateRoleCommand command) {
        security.lockAdministratorGuard();
        Set<String> ceiling = security.permissionCeiling();
        SysRole role = requireRole(mapper.lock(command.id()));
        if (!Objects.equals(role.getVersion(), command.version())) throw new ApiException(409, "角色版本已变更");
        requireWithinCeiling(role.getId(), ceiling);
        validateSelections(command.menuIds(), ceiling);
        apply(role, command.name(), command.status(), command.sort(), command.remark());
        try {
            requireOne(mapper.updateRole(role, actor()));
            replaceMenus(role.getId(), command.menuIds());
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "角色关联关系冲突");
        }
        security.requireUsableAdministrator();
        role.setVersion(role.getVersion() + 1);
        audit.platformSuccess("role", role.getId(), "UPDATE");
        return RoleView.from(role);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void delete(String id, int version) {
        security.lockAdministratorGuard();
        Set<String> ceiling = security.permissionCeiling();
        SysRole role = requireRole(mapper.lock(id));
        if (!Objects.equals(role.getVersion(), version)) throw new ApiException(409, "角色版本已变更");
        requireWithinCeiling(id, ceiling);
        if (mapper.hasUserGrant(id)) throw new ApiException(409, "角色仍授予有效账号，不能删除");
        mapper.removeMenus(id);
        requireOne(mapper.softDelete(id, version, actor()));
        security.requireUsableAdministrator();
        audit.platformSuccess("role", id, "DELETE");
    }

    @Transactional(readOnly = true)
    public IPage<AccountView> accounts(RoleQuery query) {
        security.requireAllDataRange();
        Page<AccountRow> page = new Page<>(query.page(), query.size());
        page.setOptimizeCountSql(false);
        return mapper.accounts(page, query.search()).convert(row -> new AccountView(
                row.id(), row.loginName(), row.realName(), row.status(), mapper.accountRoleIds(row.id())));
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void assign(String userId, AssignRolesCommand command) {
        security.lockAdministratorGuard();
        Set<String> ceiling = security.permissionCeiling();
        security.requireAllDataRange();
        assignAuthorized(userId, command.roleIds(), ceiling);
    }

    /** 仅供已经校验目标用户数据范围的授权服务调用，统一锁序和最后管理员保护。 */
    void assignAuthorized(String userId, List<String> roleIds, Set<String> ceiling) {
        requireDistinct(roleIds, "角色不能重复");
        security.lockAdministratorGuard();
        AccountRow account = mapper.lockAccount(userId);
        if (account == null || account.status() != 1) throw new ApiException(409, "账号不存在或已停用");
        if (!withinCeiling(mapper.accountRoleIds(userId), ceiling)) {
            throw new ApiException(403, "目标账号现有权限超出当前操作者的授予上限");
        }
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = mapper.lockRoles(roleIds);
            if (roles.size() != roleIds.size() || !withinCeiling(roleIds, ceiling)) {
                throw new ApiException(403, "角色超出当前操作者的授予上限");
            }
        }
        mapper.removeAccountRoles(userId);
        for (String roleId : roleIds) {
            requireOne(mapper.addAccountRole(IdWorker.getIdStr(), userId, roleId, actor()));
        }
        security.requireUsableAdministrator();
        audit.platformSuccess("account-role", userId, "GRANT");
    }

    private void validateSelections(List<String> menuIds, Set<String> ceiling) {
        requireDistinct(menuIds, "菜单权限不能重复");
        if (menuIds.isEmpty()) return;
        List<RoleMenuOption> selected = mapper.lockMenus(menuIds);
        if (selected.size() != menuIds.size()
                || selected.stream().anyMatch(menu -> menu.type() == 3 && !ceiling.contains(menu.code()))) {
            throw new ApiException(403, "权限超出当前操作者的授予上限");
        }
    }

    private boolean withinCeiling(List<String> roleIds, Set<String> ceiling) {
        if (roleIds.isEmpty()) return true;
        return mapper.permissions(roleIds).stream()
                .noneMatch(permission -> !ceiling.contains(permission.code()));
    }

    private void requireWithinCeiling(String roleId, Set<String> ceiling) {
        if (!withinCeiling(List.of(roleId), ceiling)) {
            throw new ApiException(403, "现有角色权限超出当前操作者的授予上限");
        }
    }

    private void replaceMenus(String roleId, List<String> menuIds) {
        mapper.removeMenus(roleId);
        for (String menuId : menuIds) requireOne(mapper.addMenu(IdWorker.getIdStr(), roleId, menuId, actor()));
    }

    private static void apply(SysRole role, String name, int status, int sort, String remark) {
        role.setName(name);
        role.setStatus(status);
        role.setSort(sort);
        role.setRemark(remark);
    }

    private static void requireDistinct(List<String> ids, String message) {
        if (new HashSet<>(ids).size() != ids.size()) throw new ApiException(400, message);
    }

    private static SysRole requireRole(SysRole role) {
        if (role == null) throw new ApiException(404, "角色不存在");
        return role;
    }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "角色记录或关联关系已变更");
    }

    private static String actor() { return ScopeContext.get().loginId(); }
}
