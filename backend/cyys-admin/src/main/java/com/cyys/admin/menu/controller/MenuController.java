package com.cyys.admin.menu.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.menu.dto.CreateMenuCommand;
import com.cyys.admin.menu.dto.UpdateMenuCommand;
import com.cyys.admin.menu.service.MenuService;
import com.cyys.common.web.R;
import com.cyys.common.web.PlatformControl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@PlatformControl
@RequestMapping("/api/admin/platform/menus")
public class MenuController {
    private final MenuService service;

    @GetMapping
    @SaCheckPermission("menu:read")
    public Object all() { return R.ok(service.all()); }

    @GetMapping("/{id}")
    @SaCheckPermission("menu:read")
    public Object detail(@PathVariable @NotBlank @Size(max = 32) String id) {
        return R.ok(service.detail(id));
    }

    @PostMapping
    @SaCheckPermission("menu:create")
    public Object create(@Valid @RequestBody CreateMenuCommand command) {
        return R.ok(service.create(command));
    }

    @PutMapping
    @SaCheckPermission("menu:update")
    public Object edit(@Valid @RequestBody UpdateMenuCommand command) {
        return R.ok(service.edit(command));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("menu:delete")
    public Object delete(@PathVariable @NotBlank @Size(max = 32) String id,
                         @RequestParam @Min(0) int version) {
        service.delete(id, version);
        return R.ok();
    }
}
