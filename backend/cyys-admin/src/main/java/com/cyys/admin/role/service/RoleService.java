package com.cyys.admin.role.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.role.dto.RoleDTO;
import com.cyys.admin.role.mapper.RoleGrantMapper;
import com.cyys.admin.role.mapper.SysRoleMapper;
import com.cyys.admin.role.model.SysRole;
import com.cyys.admin.user.mapper.SysUserRoleMapper;
import com.cyys.common.mybatis.DataAccess;
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
    private final SysUserRoleMapper userRoles;
    private final DataAuthorizationMapper authorization;
    private final DataAuditService audit;

    @Transactional(readOnly = true)
    public IPage<RoleDTO.View> page(@Valid RoleDTO.Query query) {
        authorize("role:read", false);
        Page<SysRole> page = new Page<>(query.page(), query.size());
        page.setOptimizeCountSql(false);
        return mapper.page(page, scope(), query.search()).convert(RoleDTO.View::from);
    }

    @Transactional(readOnly = true)
    public RoleDTO.Detail detail(String id) {
        Set<String> ceiling = authorize("role:read", false);
        SysRole role = requireRole(mapper.detail(id, scope()));
        List<String> menuIds = mapper.menuIds(role.getId());
        boolean editable = withinCeiling(List.of(role.getId()), ceiling);
        return new RoleDTO.Detail(RoleDTO.View.from(role), menuIds, mapper.orgIds(role.getId(), scope()), editable);
    }

    @Transactional(readOnly = true)
    public RoleDTO.Options optionsForCreate() {
        Set<String> ceiling = authorize("role:create", true);
        return options(ceiling);
    }

    @Transactional(readOnly = true)
    public RoleDTO.Options optionsForUpdate() {
        Set<String> ceiling = authorize("role:update", true);
        return options(ceiling);
    }

    @Transactional
    public RoleDTO.View create(@Valid RoleDTO.Create command) {
        Set<String> ceiling = authorize("role:create", true);
        validateSelections(command.dataRange(), command.menuIds(), command.orgIds(), ceiling);
        var role = new SysRole();
        role.setId(IdWorker.getIdStr());
        role.setScopeId(scope());
        role.setCode(command.code());
        apply(role, command.name(), command.dataRange(), command.status(), command.sort(), command.remark());
        try {
            requireOne(mapper.create(role, actor()));
            replaceRelations(role.getId(), command.menuIds(), command.orgIds());
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "角色编码或关联关系冲突");
        }
        audit.success("role", role.getId(), "CREATE");
        return RoleDTO.View.from(role);
    }

    @Transactional
    public RoleDTO.View edit(@Valid RoleDTO.Edit command) {
        Set<String> ceiling = authorize("role:update", true);
        SysRole role = requireRole(mapper.lock(command.id(), scope()));
        if (!Objects.equals(role.getVersion(), command.version())) throw new ApiException(409, "角色版本已变更");
        requireExistingWithinCeiling(role.getId(), ceiling);
        validateSelections(command.dataRange(), command.menuIds(), command.orgIds(), ceiling);
        apply(role, command.name(), command.dataRange(), command.status(), command.sort(), command.remark());
        try {
            requireOne(mapper.updateRole(role, actor()));
            replaceRelations(role.getId(), command.menuIds(), command.orgIds());
        } catch (DataIntegrityViolationException exception) {
            throw new ApiException(409, "角色关联关系冲突");
        }
        role.setVersion(role.getVersion() + 1);
        audit.success("role", role.getId(), "UPDATE");
        return RoleDTO.View.from(role);
    }

    @Transactional
    public void delete(String id, int version) {
        Set<String> ceiling = authorize("role:delete", true);
        SysRole role = requireRole(mapper.lock(id, scope()));
        if (!Objects.equals(role.getVersion(), version)) throw new ApiException(409, "角色版本已变更");
        requireExistingWithinCeiling(role.getId(), ceiling);
        if (mapper.hasUserGrant(role.getId(), scope())) throw new ApiException(409, "角色仍授予有效成员，不能删除");
        mapper.removeMenus(role.getId());
        mapper.removeDepartments(role.getId(), scope());
        requireOne(mapper.softDelete(role.getId(), scope(), version, actor()));
        audit.success("role", role.getId(), "DELETE");
    }

    private RoleDTO.Options options(Set<String> ceiling) {
        List<RoleDTO.MenuOption> menus = mapper.menus().stream()
                .filter(menu -> menu.type() != 3 || ceiling.contains(menu.code()))
                .toList();
        return new RoleDTO.Options(menus, mapper.departments(scope()));
    }

    private void validateSelections(int dataRange, List<String> menuIds, List<String> orgIds, Set<String> ceiling) {
        requireDistinct(menuIds, "菜单权限不能重复");
        requireDistinct(orgIds, "数据范围部门不能重复");
        if (!menuIds.isEmpty()) {
            List<RoleDTO.MenuOption> selected = mapper.lockMenus(menuIds);
            if (selected.size() != menuIds.size()
                    || selected.stream().anyMatch(menu -> menu.type() == 3 && !ceiling.contains(menu.code()))) {
                throw new ApiException(403, "菜单权限超出当前操作者的授予上限");
            }
        }
        if (dataRange == 2) {
            if (orgIds.isEmpty()) throw new ApiException(400, "自定义部门范围至少选择一个部门");
            if (mapper.lockDepartments(orgIds, scope()).size() != orgIds.size()) {
                throw new ApiException(403, "数据范围包含其他单位或无效部门");
            }
        } else if (!orgIds.isEmpty()) {
            throw new ApiException(400, "仅自定义部门范围可以提交部门");
        }
    }

    private void replaceRelations(String roleId, List<String> menuIds, List<String> orgIds) {
        mapper.removeMenus(roleId);
        for (String menuId : menuIds) requireOne(mapper.addMenu(IdWorker.getIdStr(), roleId, menuId, actor()));
        mapper.removeDepartments(roleId, scope());
        for (String orgId : orgIds) requireOne(mapper.addDepartment(roleId, scope(), orgId));
    }

    private Set<String> authorize(String permission, boolean grantOperation) {
        var identity = ScopeContext.get();
        String scope = identity.requireScopeId();
        DataTransactionGuard.bind(identity, DataAccess.View.CURRENT_SCOPE);
        if (!authorization.hasPermission(identity.loginId(), scope, permission)) {
            throw new ApiException(403, "没有当前单位的角色管理权限");
        }
        Set<String> ceiling = Set.copyOf(userRoles.selectUnitWidePermissionCodes(identity.loginId(), List.of(scope)));
        if (!ceiling.contains(permission) || (grantOperation && !ceiling.contains("role:grant"))) {
            throw new ApiException(403, "局部数据范围不能管理角色授权");
        }
        return ceiling;
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

    private static void apply(SysRole role, String name, int dataRange, int status, int sort, String remark) {
        role.setName(name);
        role.setDataRange(dataRange);
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
