package com.cyys.admin.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.role.dto.RoleDTO;
import com.cyys.admin.role.service.RoleService;
import com.cyys.common.web.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/admin/roles")
public class RoleController {
    private final RoleService service;

    @GetMapping
    @SaCheckPermission("role:read")
    public Object page(@RequestParam(defaultValue = "1") long page,
                       @RequestParam(defaultValue = "20") long size,
                       @RequestParam(required = false) String search) {
        return R.ok(service.page(new RoleDTO.Query(page, size, search)));
    }

    @GetMapping("/create-options")
    @SaCheckPermission("role:create")
    public Object createOptions() { return R.ok(service.optionsForCreate()); }

    @GetMapping("/update-options")
    @SaCheckPermission("role:update")
    public Object updateOptions() { return R.ok(service.optionsForUpdate()); }

    @GetMapping("/{id}")
    @SaCheckPermission("role:read")
    public Object detail(@PathVariable @NotBlank @Size(max = 32) String id) {
        return R.ok(service.detail(id));
    }

    @PostMapping
    @SaCheckPermission("role:create")
    public Object create(@Valid @RequestBody RoleDTO.Create command) {
        return R.ok(service.create(command));
    }

    @PutMapping
    @SaCheckPermission("role:update")
    public Object edit(@Valid @RequestBody RoleDTO.Edit command) {
        return R.ok(service.edit(command));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("role:delete")
    public Object delete(@PathVariable @NotBlank @Size(max = 32) String id,
                         @RequestParam @Min(0) int version) {
        service.delete(id, version);
        return R.ok();
    }
}
