package @@PACKAGE@@.controller;

import @@PACKAGE@@.dto.*;
import @@PACKAGE@@.service.@@CLASS@@Service;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.common.web.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/business/@@FEATURE@@")
public class @@CLASS@@Controller {
    private final @@CLASS@@Service service;
    @GetMapping @SaCheckPermission("@@FEATURE@@:read")
    public Object page(@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(200) int size@@SEARCH_ARGS@@) { return R.ok(service.page(page,size@@SEARCH_PASS@@)); }
    @GetMapping("/{id}") @SaCheckPermission("@@FEATURE@@:read")
    public Object detail(@PathVariable String id) { return R.ok(service.detail(id)); }
    @GetMapping("/choices") @SaCheckPermission("@@FEATURE@@:read")
    public Object choices() { return R.ok(service.choices()); }
    @GetMapping("/create-options") @SaCheckPermission("@@FEATURE@@:create")
    public Object createOptions() { return R.ok(service.options(null)); }
    @GetMapping("/{id}/update-options") @SaCheckPermission("@@FEATURE@@:update")
    public Object updateOptions(@PathVariable String id) { return R.ok(service.options(id)); }
    @PostMapping @SaCheckPermission("@@FEATURE@@:create")
    public Object create(@Valid @RequestBody Create@@CLASS@@Command command) { return R.ok(service.create(command)); }
    @PutMapping("/{id}") @SaCheckPermission("@@FEATURE@@:update")
    public Object update(@PathVariable String id,@Valid @RequestBody Update@@CLASS@@Command command) { return R.ok(service.update(id,command)); }
    @DeleteMapping("/{id}") @SaCheckPermission("@@FEATURE@@:delete")
    public Object remove(@PathVariable String id,@RequestParam @Min(0) int version) { service.remove(id,version);return R.ok(); }
}
