package com.cyys.admin.release.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.release.dto.*;
import com.cyys.admin.release.service.ReleaseService;
import com.cyys.common.web.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

@RestController
@Validated
@RequiredArgsConstructor
@PlatformControl
@RequestMapping("/api/admin/platform/releases")
public class ReleaseController {
    private final ReleaseService service;

    @GetMapping @SaCheckPermission("release:read")
    public Object page(@RequestParam(defaultValue="1") @Min(1) int page,
                       @RequestParam(defaultValue="20") @Min(1) @Max(200) int size,
                       @RequestParam(required=false) @Size(max=10) String number) {
        return R.ok(service.page(page,size,number));
    }
    @GetMapping("/{id}") @SaCheckPermission("release:read")
    public Object detail(@PathVariable String id) { return R.ok(service.detail(id)); }

    @GetMapping("/create-options") @SaCheckPermission("release:create")
    public Object createOptions() { return R.ok(service.options(null)); }

    @GetMapping("/{id}/edit-options") @SaCheckPermission("release:update")
    public Object editOptions(@PathVariable String id) { return R.ok(service.options(id)); }

    @GetMapping("/create-files") @SaCheckPermission("release:create")
    public Object createFiles(@RequestParam(defaultValue="1") @Min(1) int page,
                              @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
                              @RequestParam(required=false) @Size(max=200) String search) {
        return R.ok(service.fileOptions(null,page,size,search));
    }
    @GetMapping("/{id}/edit-files") @SaCheckPermission("release:update")
    public Object editFiles(@PathVariable String id,@RequestParam(defaultValue="1") @Min(1) int page,
                            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
                            @RequestParam(required=false) @Size(max=200) String search) {
        return R.ok(service.fileOptions(id,page,size,search));
    }
    @PostMapping @SaCheckPermission("release:create")
    public Object create(@Valid @RequestBody CreateReleaseCommand command) { return R.ok(service.create(command)); }

    @PutMapping("/{id}") @SaCheckPermission("release:update")
    public Object update(@PathVariable String id,@Valid @RequestBody UpdateReleaseCommand command) { return R.ok(service.update(id,command)); }

    @DeleteMapping("/{id}") @SaCheckPermission("release:delete")
    public Object delete(@PathVariable String id,@RequestParam @Min(0) int version) {
        service.delete(id,version); return R.ok();
    }
    @GetMapping("/{id}/attachments/{file}") @SaCheckPermission("release:read")
    public void download(@PathVariable String id,@PathVariable String file,HttpServletResponse response) throws IOException {
        try(var content=service.download(id,file)) {
            DownloadResponse.send(response,content.name(),content.bytes(),content.stream());
        }
    }
}
