package com.cyys.admin.region.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.region.dto.*;
import com.cyys.admin.region.service.RegionService;
import com.cyys.common.web.PlatformControl;
import com.cyys.common.web.R;
import com.cyys.common.web.DownloadResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Validated
@PlatformControl
@RequiredArgsConstructor
@RequestMapping("/api/admin/platform/regions")
public class RegionController {
    private final RegionService service;

    @PostMapping("/export") @SaCheckPermission("region:export")
    public void export(@Valid @RequestBody ExportRegionsCommand command, HttpServletResponse response) throws IOException {
        byte[] content = service.export(command);
        try (var input = new ByteArrayInputStream(content)) {
            DownloadResponse.send(response, "地区.xlsx", content.length, input);
        }
    }

    @GetMapping @SaCheckPermission("region:read")
    public Object page(@RequestParam(defaultValue="1") @Min(1) int page,
                       @RequestParam(defaultValue="20") @Min(1) @Max(200) int size,
                       @RequestParam(required=false) @Size(max=32) String parentId,
                       @RequestParam(required=false) @Size(max=20) String name) {
        return R.ok(service.page(page,size,parentId,name));
    }
    @GetMapping("/{id}") @SaCheckPermission("region:read")
    public Object detail(@PathVariable String id) { return R.ok(service.detail(id)); }

    @PostMapping @SaCheckPermission("region:create")
    public Object create(@Valid @RequestBody CreateRegionCommand command) { return R.ok(service.create(command)); }

    @PutMapping("/{id}") @SaCheckPermission("region:update")
    public Object update(@PathVariable String id,@Valid @RequestBody UpdateRegionCommand command) {
        return R.ok(service.update(id,command));
    }
    @DeleteMapping("/{id}") @SaCheckPermission("region:delete")
    public Object delete(@PathVariable String id,@RequestParam @Min(0) int version) {
        service.delete(List.of(new RegionVersionReference(id,version))); return R.ok();
    }
    @PostMapping("/delete") @SaCheckPermission("region:delete")
    public Object deleteBatch(@RequestBody @NotEmpty @Size(max=100) List<@Valid @NotNull RegionVersionReference> commands) {
        service.delete(commands); return R.ok();
    }
}
