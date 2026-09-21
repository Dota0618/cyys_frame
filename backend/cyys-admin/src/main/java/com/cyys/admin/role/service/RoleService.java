package com.cyys.admin.role.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.UnitAdministrationService;
import com.cyys.admin.role.dto.RoleQuery;
import com.cyys.admin.role.dto.CreateRoleCommand;
import com.cyys.admin.role.dto.UpdateRoleCommand;
import com.cyys.admin.role.dto.RoleView;
import com.cyys.admin.role.dto.RoleMenuOption;
import com.cyys.admin.role.dto.RoleOptions;
import com.cyys.admin.role.dto.RoleDetail;
import com.cyys.admin.role.mapper.RoleGrantMapper;
import com.cyys.admin.role.mapper.SysRoleMapper;
import com.cyys.admin.role.model.SysRole;
import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.mybatis.DataTransactionGuard;
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
public class RoleService {
    private final SysRoleMapper mapper;
    private final RoleGrantMapper grantMapper;
    private final UnitAdministrationService administration;
    private final DataAuditService audit;

    @Transactional(readOnly = true)
    public IPage<RoleView> page(@Valid RoleQuery query) {
        authorize("role:read", false);
        Page<SysRole> page = new Page<>(query.page(), query.size());
        page.setOptimizeCountSql(false);
        return mapper.page(page, scope(), query.search()).convert(RoleView::from);
    }

    @Transactional(readOnly = true)
    public RoleDetail detail(String id) {
        Set<String> ceiling = authorize("role:read", false);
        SysRole role = requireRole(mapper.detail(id, scope()));
        List<String> menuIds = mapper.menuIds(role.getId());
        boolean editable = withinCeiling(List.of(role.getId()), ceiling);
        return new RoleDetail(RoleView.from(role), menuIds, editable);
    }

    @Transactional(readOnly = true)
    public RoleOptions optionsForCreate() {
        Set<String> ceiling = authorize("role:create", true);
        return options(ceiling);
    }

    @Transactional(readOnly = true)
    public RoleOptions optionsForUpdate() {
        Set<String> ceiling = authorize("role:update", true);
        return options(ceiling);
    }

    @Transactional
    public RoleView create(@Valid CreateRoleCommand command) {
        Set<String> ceiling = authorize("role:create", true);
        validateSelections(command.menuIds(), ceiling);
        var role = new SysRole();
        role.setId(IdWorker.getIdStr());
        role.setScopeId(scope());
        role.setCode(command.code());
        apply(role, command.name(), command.status(), command.sort(), command.remark());
        try {
            requireOne(mapper.create(role, actor()));
            replaceRelations(role.getId(), command.menuIds());
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "角色编码或关联关系冲突");
        }
        audit.success("role", role.getId(), "CREATE");
        return RoleView.from(role);
    }

    @Transactional
    public RoleView edit(@Valid UpdateRoleCommand command) {
        Set<String> ceiling = authorize("role:update", true);
        SysRole role = requireRole(mapper.lock(command.id(), scope()));
        if (!Objects.equals(role.getVersion(), command.version())) throw new ApiException(409, "角色版本已变更");
        requireExistingWithinCeiling(role.getId(), ceiling);
        validateSelections(command.menuIds(), ceiling);
        apply(role, command.name(), command.status(), command.sort(), command.remark());
        try {
            requireOne(mapper.updateRole(role, actor()));
            replaceRelations(role.getId(), command.menuIds());
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "角色关联关系冲突");
        }
        role.setVersion(role.getVersion() + 1);
        audit.success("role", role.getId(), "UPDATE");
        return RoleView.from(role);
    }

    @Transactional
    public void delete(String id, int version) {
        Set<String> ceiling = authorize("role:delete", true);
        SysRole role = requireRole(mapper.lock(id, scope()));
        if (!Objects.equals(role.getVersion(), version)) throw new ApiException(409, "角色版本已变更");
        requireExistingWithinCeiling(role.getId(), ceiling);
        if (mapper.hasUserGrant(role.getId(), scope())) throw new ApiException(409, "角色仍授予有效成员，不能删除");
        mapper.removeMenus(role.getId());
        requireOne(mapper.softDelete(role.getId(), scope(), version, actor()));
        audit.success("role", role.getId(), "DELETE");
    }

    private RoleOptions options(Set<String> ceiling) {
        List<RoleMenuOption> menus = mapper.menus().stream()
                .filter(menu -> menu.type() != 3 || ceiling.contains(menu.code()))
                .toList();
        return new RoleOptions(menus);
    }

    private void validateSelections(List<String> menuIds, Set<String> ceiling) {
        requireDistinct(menuIds, "菜单权限不能重复");
        if (!menuIds.isEmpty()) {
            List<RoleMenuOption> selected = mapper.lockMenus(menuIds);
            if (selected.size() != menuIds.size()
                    || selected.stream().anyMatch(menu -> menu.type() == 3 && !ceiling.contains(menu.code()))) {
                throw new ApiException(403, "菜单权限超出当前操作者的授予上限");
            }
        }
    }

    private void replaceRelations(String roleId, List<String> menuIds) {
        mapper.removeMenus(roleId);
        for (String menuId : menuIds) requireOne(mapper.addMenu(IdWorker.getIdStr(), roleId, menuId, actor()));
    }

    private Set<String> authorize(String permission, boolean grantOperation) {
        return administration.authorize(permission, grantOperation);
    }

    private boolean withinCeiling(List<String> roleIds, Set<String> ceiling) {
        return grantMapper.permissions(roleIds).stream()
                .noneMatch(permission -> permission.platformOnly() || !ceiling.contains(permission.code()));
    }

    private void requireExistingWithinCeiling(String roleId, Set<String> ceiling) {
        if (!withinCeiling(List.of(roleId), ceiling)) {
            throw new ApiException(403, "现有角色权限超出当前操作者的授予上限");
        }
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
        if (role == null) throw new ApiException(404, "角色不存在或不可访问");
        return role;
    }

    private static void requireOne(int count) {
        if (count != 1) throw new ApiException(409, "角色记录或关联关系已变更");
    }

    private static String actor() { return ScopeContext.get().loginId(); }
    private static String scope() { return ScopeContext.get().requireScopeId(); }
}
