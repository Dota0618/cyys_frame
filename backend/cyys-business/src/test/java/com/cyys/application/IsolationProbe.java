package com.cyys.application;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.department.dto.CreateDepartmentCommand;
import com.cyys.admin.department.dto.UpdateDepartmentCommand;
import com.cyys.admin.department.dto.DepartmentVersionedId;
import com.cyys.admin.department.dto.DepartmentQuery;
import com.cyys.admin.department.service.DepartmentService;
import com.cyys.admin.role.service.RoleGrantService;
import com.cyys.admin.user.service.UserDisplayService;
import com.cyys.common.web.AuthorizedScopesRead;
import com.cyys.common.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/** 仅测试类路径的 HTTP 驱动；不会进入发布 JAR。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/isolation")
public class IsolationProbe {
    private final DepartmentService departments;
    private final IsolationProbeMapper sql;
    private final UnregisteredPolicyProbeMapper unregistered;
    private final UnknownResourceProbeMapper unknownResource;
    private final RoleGrantService grants;
    private final UserDisplayService users;
    private final com.cyys.admin.cache.service.CacheManagementService caches;

    @GetMapping("/departments")
    @SaCheckPermission("department:read")
    Object page(@RequestParam(defaultValue="1") long page, @RequestParam(defaultValue="20") long size,
                @RequestParam(required=false) String search) {
        return R.ok(departments.page(new DepartmentQuery(page,size,search)));
    }
    @GetMapping("/departments/{id}")
    @SaCheckPermission("department:read")
    Object detail(@PathVariable String id) { return R.ok(departments.detail(id)); }
    @PostMapping("/departments")
    @SaCheckPermission("department:create")
    Object create(@Valid @RequestBody CreateDepartmentCommand command) { return R.ok(departments.create(command)); }
    @PostMapping("/batch-create")
    @SaCheckPermission("department:create")
    Object createBatch(@RequestBody List<CreateDepartmentCommand> commands) { return R.ok(departments.createBatch(commands)); }
    @PutMapping("/departments")
    @SaCheckPermission("department:update")
    Object edit(@Valid @RequestBody UpdateDepartmentCommand command) { return R.ok(departments.edit(command)); }
    @PostMapping("/batch-delete")
    @SaCheckPermission("department:delete")
    Object delete(@RequestBody List<DepartmentVersionedId> commands) { departments.deleteBatch(commands); return R.ok(); }
    @GetMapping("/global")
    @AuthorizedScopesRead
    @SaCheckPermission("department:read")
    Object global() { return R.ok(departments.acrossAuthorizedScopes()); }
    @GetMapping("/sql/{shape}")
    @SaCheckPermission("department:read")
    Object sql(@PathVariable String shape) {
        return R.ok(switch(shape) {
            case "alias" -> sql.aliases(); case "left" -> sql.leftJoin(); case "subquery" -> sql.subquery();
            case "union" -> sql.union(); case "group" -> sql.groups(); case "count" -> sql.customCount();
            case "scalar" -> sql.scalarSubquery(); case "uncovered" -> sql.uncoveredSubquery();
            case "malformed" -> sql.malformed(); case "table" -> sql.unregisteredTable();
            case "policy" -> unregistered.rows(); case "ignored" -> sql.ignoredPolicy();
            case "resource" -> unknownResource.rows(); case "raw" -> sql.rawStatement();
            default -> throw new IllegalArgumentException("Unknown test shape");
        });
    }
    @PostMapping("/unsafe/{shape}")
    @SaCheckPermission("department:update")
    Object unsafe(@PathVariable String shape) {
        return R.ok(switch(shape) {
            case "unit" -> sql.unitWideUpdate("scope-a");
            case "owner" -> sql.changeOwner("g1-a-root", "scope-b");
            case "or" -> sql.disjunctiveWrite("g1-a-root");
            default -> throw new IllegalArgumentException("Unknown test shape");
        });
    }
    @PostMapping("/roles/{user}")
    Object grant(@PathVariable String user, @RequestBody List<String> roles) { grants.replaceRoles(user,roles); return R.ok(); }
    @GetMapping("/user-names")
    @SaCheckPermission("user:display:read")
    Object names(@RequestParam List<String> ids) { return R.ok(users.names(ids)); }
    @PostMapping("/user-cache-refresh")
    Object refresh() { caches.refresh("user-display"); return R.ok(); }
}
