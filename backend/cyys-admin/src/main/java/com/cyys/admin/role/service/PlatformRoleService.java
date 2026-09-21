package com.cyys.admin.role.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.role.dto.PlatformRoleQuery;
import com.cyys.admin.role.dto.CreatePlatformRoleCommand;
import com.cyys.admin.role.dto.UpdatePlatformRoleCommand;
import com.cyys.admin.role.dto.AssignPlatformRolesCommand;
import com.cyys.admin.role.dto.PlatformRoleView;
import com.cyys.admin.role.dto.PlatformRoleMenuOption;
import com.cyys.admin.role.dto.PlatformRoleOptions;
import com.cyys.admin.role.dto.PlatformRoleDetail;
import com.cyys.admin.role.dto.PlatformAccountRow;
import com.cyys.admin.role.dto.PlatformAccountView;
import com.cyys.admin.role.mapper.PlatformRoleMapper;
import com.cyys.admin.role.model.SysRole;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import java.util.*;

@Service
@Validated
@RequiredArgsConstructor
public class PlatformRoleService {
    private final PlatformRoleMapper mapper;
    private final PlatformSecurityService security;
    private final DataAuditService audit;

    @Transactional(readOnly = true)
    public IPage<PlatformRoleView> page(@Valid PlatformRoleQuery query) {
        security.authorize("platform:role:read");
        Page<SysRole> page = new Page<>(query.page(), query.size());
        page.setOptimizeCountSql(false);
        return mapper.page(page, query.search()).convert(PlatformRoleView::from);
    }

    @Transactional(readOnly = true)
    public PlatformRoleDetail detail(String id) {
        Set<String> ceiling = security.authorize("platform:role:read");
        SysRole role = requireRole(mapper.detail(id));
        return new PlatformRoleDetail(PlatformRoleView.from(role), mapper.menuIds(id),
                withinCeiling(List.of(id), ceiling));
    }

    @Transactional(readOnly = true)
    public PlatformRoleOptions options(String permission) {
        Set<String> ceiling = security.authorize(permission);
        return new PlatformRoleOptions(mapper.menus().stream()
                .filter(menu -> menu.type() != 3 || ceiling.contains(menu.code()))
                .toList());
    }

    @Transactional
    public PlatformRoleView create(@Valid CreatePlatformRoleCommand command) {
        Set<String> ceiling = security.authorize("platform:role:create");
        validateSelections(command.menuIds(), ceiling);
        var role = new SysRole();
        role.setId(IdWorker.getIdStr());
        role.setScopeId(PlatformSecurityService.GLOBAL);
        role.setCode(command.code());
        apply(role, command.name(), command.status(), command.sort(), command.remark());
        try {
            requireOne(mapper.create(role, actor()));
            replaceMenus(role.getId(), command.menuIds());
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "平台角色编码或关联关系冲突");
        }
        audit.platformSuccess("platform-role", role.getId(), "CREATE");
        return PlatformRoleView.from(role);
    }

    @Transactional
    public PlatformRoleView edit(@Valid UpdatePlatformRoleCommand command) {
        Set<String> ceiling = security.authorize("platform:role:update");
        security.lockAdministratorGuard();
        SysRole role = requireRole(mapper.lock(command.id()));
        if (!Objects.equals(role.getVersion(), command.version())) throw new ApiException(409, "平台角色版本已变更");
        requireWithinCeiling(role.getId(), ceiling);
        validateSelections(command.menuIds(), ceiling);
        apply(role, command.name(), command.status(), command.sort(), command.remark());
        try {
            requireOne(mapper.updateRole(role, actor()));
            replaceMenus(role.getId(), command.menuIds());
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "平台角色关联关系冲突");
        }
        security.requireUsableAdministrator();
        role.setVersion(role.getVersion() + 1);
        audit.platformSuccess("platform-role", role.getId(), "UPDATE");
        return PlatformRoleView.from(role);
    }

    @Transactional
    public void delete(String id, int version) {
        Set<String> ceiling = security.authorize("platform:role:delete");
        security.lockAdministratorGuard();
        SysRole role = requireRole(mapper.lock(id));
        if (!Objects.equals(role.getVersion(), version)) throw new ApiException(409, "平台角色版本已变更");
        requireWithinCeiling(id, ceiling);
        if (mapper.hasUserGrant(id)) throw new ApiException(409, "平台角色仍授予有效账号，不能删除");
        mapper.removeMenus(id);
        requireOne(mapper.softDelete(id, version, actor()));
        security.requireUsableAdministrator();
        audit.platformSuccess("platform-role", id, "DELETE");
    }

    @Transactional(readOnly = true)
    public IPage<PlatformAccountView> accounts(@Valid PlatformRoleQuery query) {
        security.authorize("platform:role:grant");
        Page<PlatformAccountRow> page = new Page<>(query.page(), query.size());
        page.setOptimizeCountSql(false);
        return mapper.accounts(page, query.search()).convert(row -> new PlatformAccountView(
                row.id(), row.loginName(), row.realName(), row.status(), mapper.accountRoleIds(row.id())));
    }

    @Transactional
    public void assign(String userId, @Valid AssignPlatformRolesCommand command) {
        Set<String> ceiling = security.authorize("platform:role:grant");
        requireDistinct(command.roleIds(), "平台角色不能重复");
        security.lockAdministratorGuard();
        PlatformAccountRow account = mapper.lockAccount(userId);
        if (account == null || account.status() != 1) throw new ApiException(409, "账号不存在或已停用");
        if (!command.roleIds().isEmpty()) {
            List<SysRole> roles = mapper.lockRoles(command.roleIds());
            if (roles.size() != command.roleIds().size() || !withinCeiling(command.roleIds(), ceiling)) {
                throw new ApiException(403, "平台角色超出当前操作者的授予上限");
            }
        }
        mapper.removeAccountRoles(userId);
        for (String roleId : command.roleIds()) {
            requireOne(mapper.addAccountRole(IdWorker.getIdStr(), userId, roleId, actor()));
        }
        security.requireUsableAdministrator();
        audit.platformSuccess("platform-account-role", userId, "GRANT");
    }

    private void validateSelections(List<String> menuIds, Set<String> ceiling) {
        requireDistinct(menuIds, "菜单权限不能重复");
        if (menuIds.isEmpty()) return;
        List<PlatformRoleMenuOption> selected = mapper.lockMenus(menuIds);
        if (selected.size() != menuIds.size()
                || selected.stream().anyMatch(menu -> menu.type() == 3 && !ceiling.contains(menu.code()))) {
            throw new ApiException(403, "平台权限超出当前操作者的授予上限");
        }
    }

    private boolean withinCeiling(List<String> roleIds, Set<String> ceiling) {
        if (roleIds.isEmpty()) return true;
        return mapper.permissions(roleIds).stream()
                .noneMatch(permission -> !ceiling.contains(permission.code()));
    }

    private void requireWithinCeiling(String roleId, Set<String> ceiling) {
        if (!withinCeiling(List.of(roleId), ceiling)) {
            throw new ApiException(403, "现有平台角色权限超出当前操作者的授予上限");
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
        if (role == null) throw new ApiException(404, "平台角色不存在");
        return role;
    }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "平台角色记录或关联关系已变更");
    }

    private static String actor() { return ScopeContext.get().loginId(); }
}
