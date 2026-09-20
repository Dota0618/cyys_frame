package com.cyys.admin.role.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.role.dto.PlatformRoleQuery;
import com.cyys.admin.role.dto.CreatePlatformRoleCommand;
import com.cyys.admin.role.dto.UpdatePlatformRoleCommand;
import com.cyys.admin.role.dto.AssignPlatformRolesCommand;
import com.cyys.admin.role.service.PlatformRoleService;
import com.cyys.common.web.R;
import com.cyys.common.web.PlatformControl;
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
@PlatformControl
@RequestMapping("/api/admin/platform/roles")
public class PlatformRoleController {
    private final PlatformRoleService service;

    @GetMapping
    @SaCheckPermission("platform:role:read")
    public Object page(@RequestParam(defaultValue = "1") long page,
                       @RequestParam(defaultValue = "20") long size,
                       @RequestParam(required = false) String search) {
        return R.ok(service.page(new PlatformRoleQuery(page, size, search)));
    }

    @GetMapping("/create-options")
    @SaCheckPermission("platform:role:create")
    public Object createOptions() { return R.ok(service.options("platform:role:create")); }

    @GetMapping("/update-options")
    @SaCheckPermission("platform:role:update")
    public Object updateOptions() { return R.ok(service.options("platform:role:update")); }

    @GetMapping("/accounts")
    @SaCheckPermission("platform:role:grant")
    public Object accounts(@RequestParam(defaultValue = "1") long page,
                           @RequestParam(defaultValue = "20") long size,
                           @RequestParam(required = false) String search) {
        return R.ok(service.accounts(new PlatformRoleQuery(page, size, search)));
    }

    @PutMapping("/accounts/{userId}/roles")
    @SaCheckPermission("platform:role:grant")
    public Object assign(@PathVariable @NotBlank @Size(max = 32) String userId,
                         @Valid @RequestBody AssignPlatformRolesCommand command) {
        service.assign(userId, command);
        return R.ok();
    }

    @GetMapping("/{id}")
    @SaCheckPermission("platform:role:read")
    public Object detail(@PathVariable @NotBlank @Size(max = 32) String id) {
        return R.ok(service.detail(id));
    }

    @PostMapping
    @SaCheckPermission("platform:role:create")
    public Object create(@Valid @RequestBody CreatePlatformRoleCommand command) {
        return R.ok(service.create(command));
    }

    @PutMapping
    @SaCheckPermission("platform:role:update")
    public Object edit(@Valid @RequestBody UpdatePlatformRoleCommand command) {
        return R.ok(service.edit(command));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("platform:role:delete")
    public Object delete(@PathVariable @NotBlank @Size(max = 32) String id,
                         @RequestParam @Min(0) int version) {
        service.delete(id, version);
        return R.ok();
    }
}
