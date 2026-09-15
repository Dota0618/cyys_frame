package com.cyys.admin.department.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.department.dto.DepartmentDTO;
import com.cyys.admin.department.service.DepartmentService;
import com.cyys.common.web.R;
import jakarta.validation.Valid;
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
    public Object page(@RequestParam(defaultValue = "1") long page,
                       @RequestParam(defaultValue = "20") long size,
                       @RequestParam(required = false) String search) {
        return R.ok(service.page(new DepartmentDTO.Query(page, size, search)));
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

    @GetMapping("/{id}")
    @SaCheckPermission("department:read")
    public Object detail(@PathVariable String id) {
        return R.ok(service.detail(id));
    }

    @PostMapping
    @SaCheckPermission("department:create")
    public Object create(@Valid @RequestBody DepartmentDTO.Create command) {
        return R.ok(service.create(command));
    }

    @PutMapping
    @SaCheckPermission("department:update")
    public Object edit(@Valid @RequestBody DepartmentDTO.Edit command) {
        return R.ok(service.edit(command));
    }

    @DeleteMapping
    @SaCheckPermission("department:delete")
    public Object delete(@RequestBody List<DepartmentDTO.VersionedId> commands) {
        service.deleteBatch(commands);
        return R.ok();
    }
}
