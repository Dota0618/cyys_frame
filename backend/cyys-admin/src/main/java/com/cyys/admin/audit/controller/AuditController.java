package com.cyys.admin.audit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.audit.dto.AuditOptions;
import com.cyys.admin.audit.dto.AuditQuery;
import com.cyys.admin.audit.service.AuditQueryService;
import com.cyys.common.web.R;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/admin/audits")
public class AuditController {
    private final AuditQueryService service;

    @GetMapping("/options")
    @SaCheckPermission("audit:read")
    public R<AuditOptions> options() {
        return R.ok(service.options());
    }

    @GetMapping
    @SaCheckPermission("audit:read")
    public Object page(@RequestParam(defaultValue = "1") @Min(1) long page,
                       @RequestParam(defaultValue = "20") @Min(1) @Max(200) long size,
                       @RequestParam(required = false) @Size(max = 64) String resource,
                       @RequestParam(required = false) @Size(max = 20) String operation,
                       @RequestParam(required = false) @Size(max = 20) String outcome) {
        return R.ok(service.page(new AuditQuery(page, size, resource, operation, outcome)));
    }
}
