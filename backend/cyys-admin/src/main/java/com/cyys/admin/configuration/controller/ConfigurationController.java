package com.cyys.admin.configuration.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.configuration.dto.CreateConfigurationCommand;
import com.cyys.admin.configuration.dto.UpdateConfigurationCommand;
import com.cyys.admin.configuration.service.ConfigurationService;
import com.cyys.common.web.PlatformControl;
import com.cyys.common.web.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@PlatformControl
@RequestMapping("/api/admin/platform/configurations")
public class ConfigurationController {
    private final ConfigurationService service;

    @GetMapping
    @SaCheckPermission("configuration:read")
    public Object page(@RequestParam(defaultValue = "1") @Min(1) int page,
                       @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
                       @RequestParam(required = false) @Size(max = 50) String name) {
        return R.ok(service.page(page, size, name));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("configuration:read")
    public Object detail(@PathVariable String id) { return R.ok(service.detail(id)); }

    @PostMapping
    @SaCheckPermission("configuration:create")
    public Object create(@Valid @RequestBody CreateConfigurationCommand command) {
        return R.ok(service.create(command));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("configuration:update")
    public Object update(@PathVariable String id, @Valid @RequestBody UpdateConfigurationCommand command) {
        return R.ok(service.update(id, command));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("configuration:delete")
    public Object delete(@PathVariable String id, @RequestParam int version) {
        service.delete(id, version);
        return R.ok();
    }
}
