package com.cyys.admin.recycle.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.recycle.dto.CreateRecycleConfigurationCommand;
import com.cyys.admin.recycle.dto.UpdateRecycleConfigurationCommand;
import com.cyys.admin.recycle.service.RecycleConfigurationService;
import com.cyys.common.web.PlatformControl;
import com.cyys.common.web.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@PlatformControl
@RequestMapping("/api/admin/platform/recycle-resources")
public class RecycleConfigurationController {
    private final RecycleConfigurationService service;

    @GetMapping
    @SaCheckPermission("recycle:config:read")
    public Object all() { return R.ok(service.all()); }

    @GetMapping("/tables")
    @SaCheckPermission("recycle:config:read")
    public Object tables() { return R.ok(service.tables()); }

    @PostMapping
    @SaCheckPermission("recycle:config:create")
    public Object create(@Valid @RequestBody CreateRecycleConfigurationCommand command) { return R.ok(service.create(command)); }

    @PutMapping("/{id}")
    @SaCheckPermission("recycle:config:update")
    public Object update(@PathVariable @NotBlank String id, @Valid @RequestBody UpdateRecycleConfigurationCommand command) {
        return R.ok(service.update(id, command));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("recycle:config:delete")
    public Object delete(@PathVariable @NotBlank String id, @RequestParam int version) {
        service.delete(id, version); return R.ok();
    }
}
