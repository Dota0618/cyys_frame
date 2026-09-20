package com.cyys.admin.dictionary.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.dictionary.dto.*;
import com.cyys.admin.dictionary.service.DictionaryService;
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
@RequestMapping("/api/admin/platform/dictionaries")
public class DictionaryController {
    private final DictionaryService service;

    @GetMapping
    @SaCheckPermission("dictionary:read")
    public Object page(@RequestParam(defaultValue = "1") @Min(1) int page,
                       @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
                       @RequestParam(required = false) @Size(max = 20) String name) {
        return R.ok(service.page(page, size, name));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("dictionary:read")
    public Object detail(@PathVariable String id) { return R.ok(service.detail(id)); }

    @PostMapping
    @SaCheckPermission("dictionary:create")
    public Object create(@Valid @RequestBody CreateDictionaryCommand command) {
        return R.ok(service.create(command));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("dictionary:update")
    public Object update(@PathVariable String id, @Valid @RequestBody UpdateDictionaryCommand command) {
        return R.ok(service.update(id, command));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("dictionary:delete")
    public Object delete(@PathVariable String id, @RequestParam int version) {
        service.delete(id, version);
        return R.ok();
    }

    @GetMapping("/{basicId}/items")
    @SaCheckPermission("dictionary:read")
    public Object items(@PathVariable String basicId,
                        @RequestParam(defaultValue = "1") @Min(1) int page,
                        @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
                        @RequestParam(required = false) @Size(max = 20) String paramValue) {
        return R.ok(service.items(basicId, page, size, paramValue));
    }

    @GetMapping("/{basicId}/items/{id}")
    @SaCheckPermission("dictionary:read")
    public Object item(@PathVariable String basicId, @PathVariable String id) {
        return R.ok(service.item(basicId, id));
    }

    @PostMapping("/{basicId}/items")
    @SaCheckPermission("dictionary:create")
    public Object createItem(@PathVariable String basicId, @Valid @RequestBody CreateDictionaryItemCommand command) {
        return R.ok(service.createItem(basicId, command));
    }

    @PutMapping("/{basicId}/items/{id}")
    @SaCheckPermission("dictionary:update")
    public Object updateItem(@PathVariable String basicId, @PathVariable String id,
                             @Valid @RequestBody UpdateDictionaryItemCommand command) {
        return R.ok(service.updateItem(basicId, id, command));
    }

    @DeleteMapping("/{basicId}/items/{id}")
    @SaCheckPermission("dictionary:delete")
    public Object deleteItem(@PathVariable String basicId, @PathVariable String id, @RequestParam int version) {
        service.deleteItem(basicId, id, version);
        return R.ok();
    }
}
