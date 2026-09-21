package com.cyys.admin.scope.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.scope.dto.*;
import com.cyys.admin.scope.service.ScopeManagementService;
import com.cyys.common.web.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@PlatformControl
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/admin/platform/scopes")
public class ScopeManagementController {
    private final ScopeManagementService service;
    @GetMapping("/tree") @SaCheckPermission("platform:scope:read")
    public Object tree() { return R.ok(service.tree()); }
    @GetMapping @SaCheckPermission("platform:scope:read")
    public Object page(@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(200) int size,
                       @RequestParam(required=false) @Size(max=100) String search) { return R.ok(service.page(page,size,search)); }
    @PostMapping @SaCheckPermission("platform:scope:create")
    public Object create(@Valid @RequestBody CreateScopeCommand command) { return R.ok(service.create(command)); }
    @PutMapping("/{id}") @SaCheckPermission("platform:scope:update")
    public Object update(@PathVariable @Size(max=32) String id,@Valid @RequestBody UpdateScopeCommand command) { return R.ok(service.update(id,command)); }
}
