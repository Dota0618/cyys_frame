package com.cyys.application;

import com.cyys.admin.department.dto.DepartmentDTO;
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
    private final RoleGrantService grants;
    private final UserDisplayService users;

    @GetMapping("/departments")
    Object page(@RequestParam(defaultValue="1") long page, @RequestParam(defaultValue="20") long size,
                @RequestParam(required=false) String search) {
        return R.ok(departments.page(new DepartmentDTO.Query(page,size,search)));
    }
    @GetMapping("/departments/{id}")
    Object detail(@PathVariable String id) { return R.ok(departments.detail(id)); }
    @PostMapping("/departments")
    Object create(@Valid @RequestBody DepartmentDTO.Create command) { return R.ok(departments.create(command)); }
    @PostMapping("/batch-create")
    Object createBatch(@RequestBody List<DepartmentDTO.Create> commands) { return R.ok(departments.createBatch(commands)); }
    @PutMapping("/departments")
    Object edit(@Valid @RequestBody DepartmentDTO.Edit command) { return R.ok(departments.edit(command)); }
    @PostMapping("/batch-delete")
    Object delete(@RequestBody List<DepartmentDTO.VersionedId> commands) { departments.deleteBatch(commands); return R.ok(); }
    @GetMapping("/global")
    @AuthorizedScopesRead
    Object global() { return R.ok(departments.acrossAuthorizedScopes()); }
    @GetMapping("/sql/{shape}")
    Object sql(@PathVariable String shape) {
        return R.ok(switch(shape) {
            case "alias" -> sql.aliases(); case "left" -> sql.leftJoin(); case "subquery" -> sql.subquery();
            case "union" -> sql.union(); case "group" -> sql.groups(); case "count" -> sql.customCount();
            case "scalar" -> sql.scalarSubquery(); case "uncovered" -> sql.uncoveredSubquery();
            case "malformed" -> sql.malformed(); case "table" -> sql.unregisteredTable();
            case "policy" -> sql.unregisteredPolicy(); case "ignored" -> sql.ignoredPolicy();
            case "resource" -> sql.unknownResource(); case "raw" -> sql.rawStatement();
            default -> throw new IllegalArgumentException("Unknown test shape");
        });
    }
    @PostMapping("/unsafe/{shape}")
    Object unsafe(@PathVariable String shape) {
        return R.ok(switch(shape) {
            case "unit" -> sql.unitWideUpdate("scope-a");
            case "owner" -> sql.changeOwner("g1-a-root", "scope-b");
            case "or" -> sql.disjunctiveWrite("g1-a-root");
            default -> throw new IllegalArgumentException("Unknown test shape");
        });
    }
    @PostMapping("/roles/{user}")
    Object grant(@PathVariable String user, @RequestBody List<String> roles) { grants.replaceUnitRoles(user,roles); return R.ok(); }
    @GetMapping("/user-names")
    Object names(@RequestParam List<String> ids) { return R.ok(users.names(ids)); }
    @PostMapping("/user-cache-refresh")
    Object refresh() { users.refreshCurrentUnit(); return R.ok(); }
}
