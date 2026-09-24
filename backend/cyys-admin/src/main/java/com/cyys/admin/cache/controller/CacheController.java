package com.cyys.admin.cache.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.cache.dto.CreateCacheDefinitionCommand;
import com.cyys.admin.cache.dto.RefreshCacheGroupCommand;
import com.cyys.admin.cache.dto.UpdateCacheDefinitionCommand;
import com.cyys.admin.cache.service.CacheManagementService;
import com.cyys.common.web.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/admin/caches")
public class CacheController {
    private final CacheManagementService service;

    @GetMapping
    @SaCheckPermission("cache:user:refresh")
    public Object list() { return R.ok(service.list()); }

    @PostMapping
    @SaCheckPermission("cache:user:refresh")
    public Object create(@Valid @RequestBody CreateCacheDefinitionCommand command) {
        return R.ok(service.create(command));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("cache:user:refresh")
    public Object update(@PathVariable String id, @Valid @RequestBody UpdateCacheDefinitionCommand command) {
        return R.ok(service.update(id, command));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("cache:user:refresh")
    public Object delete(@PathVariable String id, @RequestParam @Min(0) int version) {
        service.delete(id, version);
        return R.ok();
    }

    @PostMapping("/refresh-groups/refresh")
    @SaCheckPermission("cache:user:refresh")
    public Object refreshGroup(@Valid @RequestBody RefreshCacheGroupCommand command) {
        service.refreshGroup(command.refreshGroup(), command.scope());
        return R.ok();
    }
}
