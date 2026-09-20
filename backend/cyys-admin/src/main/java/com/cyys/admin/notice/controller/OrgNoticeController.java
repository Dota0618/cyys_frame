package com.cyys.admin.notice.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cyys.admin.notice.dto.*;
import com.cyys.admin.notice.service.OrgNoticeService;
import com.cyys.admin.file.dto.FileOption;
import com.cyys.common.web.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@Validated
@RestController
@RequestMapping("/api/admin/org-notices")
@RequiredArgsConstructor
public class OrgNoticeController {
    private final OrgNoticeService service;

    @GetMapping @SaCheckPermission("org-notice:read")
    public R<IPage<OrgNoticeView>> page(@RequestParam(defaultValue="1") @Min(1) long page,@RequestParam(defaultValue="20") @Min(1) @Max(100) long size,@RequestParam(required=false) @Size(max=100) String title) {
        return R.ok(service.page(page,size,title));
    }
    @GetMapping("/{id}") @SaCheckPermission("org-notice:read")
    public R<OrgNoticeDetail> detail(@PathVariable String id) { return R.ok(service.detail(id)); }
    @GetMapping("/{id}/edit") @SaCheckPermission("org-notice:update")
    public R<OrgNoticeDetail> edit(@PathVariable String id) { return R.ok(service.detail(id)); }
    @GetMapping("/create-options") @SaCheckPermission("org-notice:create")
    public R<OrgNoticeOptions> createOptions() { return R.ok(service.options(null)); }
    @GetMapping("/{id}/update-options") @SaCheckPermission("org-notice:update")
    public R<OrgNoticeOptions> updateOptions(@PathVariable String id) { return R.ok(service.options(id)); }
    @GetMapping("/create-files") @SaCheckPermission("org-notice:create")
    public R<IPage<FileOption>> createFiles(@RequestParam(defaultValue="1") @Min(1) long page,@RequestParam(defaultValue="20") @Min(1) @Max(100) long size,@RequestParam(required=false) @Size(max=200) String search) {
        return R.ok(service.fileOptions(null,page,size,search));
    }
    @GetMapping("/{id}/update-files") @SaCheckPermission("org-notice:update")
    public R<IPage<FileOption>> updateFiles(@PathVariable String id,@RequestParam(defaultValue="1") @Min(1) long page,@RequestParam(defaultValue="20") @Min(1) @Max(100) long size,@RequestParam(required=false) @Size(max=200) String search) {
        return R.ok(service.fileOptions(id,page,size,search));
    }
    @PostMapping @SaCheckPermission("org-notice:create")
    public R<OrgNoticeDetail> create(@Valid @RequestBody CreateOrgNoticeCommand command) { return R.ok(service.create(command)); }
    @PutMapping("/{id}") @SaCheckPermission("org-notice:update")
    public R<OrgNoticeDetail> update(@PathVariable String id,@Valid @RequestBody UpdateOrgNoticeCommand command) { return R.ok(service.update(id,command)); }
    @DeleteMapping("/{id}") @SaCheckPermission("org-notice:delete")
    public R<Void> remove(@PathVariable String id,@RequestParam @Min(0) int version) { service.remove(id,version); return R.ok(); }
    @PostMapping("/{id}/read") @SaCheckPermission("org-notice:read")
    public R<Void> read(@PathVariable String id) { service.markRead(id); return R.ok(); }
    @GetMapping("/{id}/readers") @SaCheckPermission("org-notice:receipts:read")
    public R<IPage<OrgNoticeReadView>> readers(@PathVariable String id,@RequestParam(defaultValue="1") @Min(1) long page,@RequestParam(defaultValue="20") @Min(1) @Max(100) long size) {
        return R.ok(service.readers(id,page,size));
    }
    @GetMapping("/{id}/attachments/{file}/content") @SaCheckPermission("org-notice:read")
    public void download(@PathVariable String id,@PathVariable String file,HttpServletResponse response) throws IOException {
        try(var download=service.download(id,file)) { DownloadResponse.send(response,download.name(),download.bytes(),download.stream()); }
    }
}
