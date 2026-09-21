package com.cyys.admin.department.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.department.dto.CreateDepartmentCommand;
import com.cyys.admin.department.dto.UpdateDepartmentCommand;
import com.cyys.admin.department.dto.DepartmentVersionedId;
import com.cyys.admin.department.dto.DepartmentQuery;
import com.cyys.admin.department.service.DepartmentService;
import com.cyys.common.web.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/departments")
public class DepartmentController {
    private final DepartmentService service;

    @GetMapping
    @SaCheckPermission("department:read")
    public Object page(@RequestParam(defaultValue = "1") @Min(1) long page,
                       @RequestParam(defaultValue = "20") @Min(1) @Max(200) long size,
                       @RequestParam(required = false) @Size(max=100) String search) {
        return R.ok(service.page(new DepartmentQuery(page, size, search)));
    }

    @GetMapping("/parents")
    @SaCheckPermission("department:create")
    public Object parents() {
        return R.ok(service.parentOptionsForCreate());
    }

    @GetMapping("/tree")
    @SaCheckPermission("department:read")
    public Object tree() {
        return R.ok(service.treeOptionsForRead());
    }

    @GetMapping("/leader-options/create")
    @SaCheckPermission("department:create")
    public Object createLeaderOptions(@RequestParam(required = false) @Size(max=100) String search) {
        return R.ok(service.leaderOptionsForCreate(search));
    }

    @GetMapping("/{id}/leader-options")
    @SaCheckPermission("department:update")
    public Object updateLeaderOptions(@PathVariable String id, @RequestParam(required = false) @Size(max=100) String search) {
        return R.ok(service.leaderOptionsForUpdate(id, search));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("department:read")
    public Object detail(@PathVariable String id) {
        return R.ok(service.detail(id));
    }

    @PostMapping
    @SaCheckPermission("department:create")
    public Object create(@Valid @RequestBody CreateDepartmentCommand command) {
        return R.ok(service.create(command));
    }

    @PutMapping
    @SaCheckPermission("department:update")
    public Object edit(@Valid @RequestBody UpdateDepartmentCommand command) {
        return R.ok(service.edit(command));
    }

    @DeleteMapping
    @SaCheckPermission("department:delete")
    public Object delete(@RequestBody @NotEmpty @Size(max=100) List<@NotNull @Valid DepartmentVersionedId> commands) {
        service.deleteBatch(commands);
        return R.ok();
    }
}
