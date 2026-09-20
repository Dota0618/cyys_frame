package com.cyys.admin.file.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.file.dto.*;
import com.cyys.admin.file.service.FileDirectoryService;
import com.cyys.common.web.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/admin/file-directories")
@RequiredArgsConstructor
public class FileDirectoryController {
    private final FileDirectoryService service;
    @GetMapping @SaCheckPermission("file:read")
    public R<List<FileDirectoryView>> list() { return R.ok(service.list()); }
    @GetMapping("/move-options") @SaCheckPermission("file:move")
    public R<List<FileDirectoryView>> moveOptions() { return R.ok(service.list()); }
    @PostMapping @SaCheckPermission("file:directory:create")
    public R<FileDirectoryView> create(@Valid @RequestBody CreateFileDirectoryCommand command) { return R.ok(service.create(command)); }
    @PutMapping("/{id}") @SaCheckPermission("file:directory:update")
    public R<FileDirectoryView> rename(@PathVariable String id,@Valid @RequestBody UpdateFileDirectoryCommand command) { return R.ok(service.rename(id,command)); }
    @DeleteMapping("/{id}") @SaCheckPermission("file:directory:delete")
    public R<Void> delete(@PathVariable String id,@RequestParam @Min(0) int version) { service.delete(id,version); return R.ok(); }
}
