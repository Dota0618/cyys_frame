package com.cyys.admin.recycle.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.recycle.dto.RecycleBatchCommand;
import com.cyys.admin.recycle.service.RecycleService;
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
@RequestMapping("/api/admin/recycle")
public class RecycleController {
    private final RecycleService service;

    @GetMapping("/resources")
    @SaCheckPermission("recycle:read")
    public Object resources() { return R.ok(service.resources()); }

    @GetMapping("/{resource}")
    @SaCheckPermission("recycle:read")
    public Object page(@PathVariable @Pattern(regexp="[a-z][a-z0-9-]{0,49}") String resource,
                       @RequestParam(defaultValue="1") @Min(1) int page,
                       @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
                       @RequestParam(required=false) @Size(max=100) String search) {
        return R.ok(service.page(resource, page, size, search));
    }

    @PostMapping("/{resource}/restore")
    @SaCheckPermission({"recycle:read", "recycle:restore"})
    public Object restore(@PathVariable @Pattern(regexp="[a-z][a-z0-9-]{0,49}") String resource,
                          @Valid @RequestBody RecycleBatchCommand command) {
        service.restore(resource, command);
        return R.ok();
    }

    @PostMapping("/{resource}/purge")
    @SaCheckPermission({"recycle:read", "recycle:purge"})
    public Object purge(@PathVariable @Pattern(regexp="[a-z][a-z0-9-]{0,49}") String resource,
                        @Valid @RequestBody RecycleBatchCommand command) {
        service.purge(resource, command);
        return R.ok();
    }
}
