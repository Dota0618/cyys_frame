package com.cyys.admin.notice.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.notice.dto.CreateSystemNoticeCommand;
import com.cyys.admin.notice.dto.UpdateSystemNoticeCommand;
import com.cyys.admin.notice.service.SystemNoticeService;
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
@RequestMapping("/api/admin/platform/system-notices")
public class SystemNoticeController {
    private final SystemNoticeService service;

    @GetMapping
    @SaCheckPermission("system-notice:read")
    public Object page(@RequestParam(defaultValue="1") @Min(1) int page,
                       @RequestParam(defaultValue="20") @Min(1) @Max(200) int size,
                       @RequestParam(required=false) @Size(max=100) String title) {
        return R.ok(service.page(page, size, title));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system-notice:read")
    public Object detail(@PathVariable String id) { return R.ok(service.detail(id)); }

    @GetMapping({"/create-options", "/{id}/edit-options"})
    @SaCheckPermission(value = {"system-notice:create", "system-notice:update"}, mode = cn.dev33.satoken.annotation.SaMode.OR)
    public Object options(@PathVariable(required = false) String id) { return R.ok(service.options(id)); }

    @PostMapping
    @SaCheckPermission("system-notice:create")
    public Object create(@Valid @RequestBody CreateSystemNoticeCommand command) { return R.ok(service.create(command)); }

    @PutMapping("/{id}")
    @SaCheckPermission("system-notice:update")
    public Object update(@PathVariable String id, @Valid @RequestBody UpdateSystemNoticeCommand command) {
        return R.ok(service.update(id, command));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system-notice:delete")
    public Object delete(@PathVariable String id, @RequestParam @Min(0) int version) {
        service.delete(id, version);
        return R.ok();
    }
}
