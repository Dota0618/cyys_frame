package com.cyys.admin.cache.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.cache.service.CacheManagementService;
import com.cyys.common.web.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/caches")
public class CacheController {
    private final CacheManagementService service;

    @GetMapping
    @SaCheckPermission("cache:user:refresh")
    public Object actions() { return R.ok(service.actions()); }

    @PostMapping("/{action}/refresh")
    @SaCheckPermission("cache:user:refresh")
    public Object refresh(@PathVariable String action) {
        service.refresh(action);
        return R.ok();
    }
}
