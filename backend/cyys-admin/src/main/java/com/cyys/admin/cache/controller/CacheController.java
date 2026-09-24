package com.cyys.admin.cache.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.cache.dto.CreateCacheRefreshItemCommand;
import com.cyys.admin.cache.dto.UpdateCacheRefreshItemCommand;
import com.cyys.admin.cache.service.CacheManagementService;
import com.cyys.admin.cache.service.CacheRefreshManagementService;
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
    private final CacheRefreshManagementService refreshService;

    @GetMapping
    @SaCheckPermission("cache:user:refresh")
    public Object actions() { return R.ok(service.actions()); }

    @GetMapping("/refresh-items")
    @SaCheckPermission("cache:user:refresh")
    public Object refreshItems() { return R.ok(refreshService.list()); }

    @PostMapping("/refresh-items")
    @SaCheckPermission("cache:user:refresh")
    public Object createRefreshItem(@Valid @RequestBody CreateCacheRefreshItemCommand command) {
        return R.ok(refreshService.create(command));
    }

    @PutMapping("/refresh-items/{id}")
    @SaCheckPermission("cache:user:refresh")
    public Object updateRefreshItem(@PathVariable String id,
                                    @Valid @RequestBody UpdateCacheRefreshItemCommand command) {
        return R.ok(refreshService.update(id, command));
    }

    @DeleteMapping("/refresh-items/{id}")
    @SaCheckPermission("cache:user:refresh")
    public Object deleteRefreshItem(@PathVariable String id, @RequestParam @Min(0) int version) {
        refreshService.delete(id, version);
        return R.ok();
    }

    @PostMapping("/refresh-items/{id}/refresh")
    @SaCheckPermission("cache:user:refresh")
    public Object refreshConfigured(@PathVariable String id) {
        refreshService.refresh(id);
        return R.ok();
    }

    @PostMapping("/{action}/refresh")
    @SaCheckPermission("cache:user:refresh")
    public Object refresh(@PathVariable String action) {
        service.refresh(action);
        return R.ok();
    }
}
