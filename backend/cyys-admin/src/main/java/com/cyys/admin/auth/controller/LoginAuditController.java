package com.cyys.admin.auth.controller;

import com.cyys.admin.auth.dto.LoginAuditQuery;
import com.cyys.admin.auth.dto.LoginAuditOptions;
import com.cyys.admin.auth.service.LoginAuditService;
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
import cn.dev33.satoken.annotation.SaCheckPermission;

@RestController
@Validated
@RequestMapping("/api/admin/login-audits")
@RequiredArgsConstructor
public class LoginAuditController {
    private final LoginAuditService service;

    @GetMapping("/options")
    @SaCheckPermission("login-audit:read")
    public R<LoginAuditOptions> options() {
        return R.ok(service.options());
    }

    @GetMapping
    @SaCheckPermission("login-audit:read")
    public R<?> page(@RequestParam(defaultValue = "1") @Min(1) long page,
                     @RequestParam(defaultValue = "20") @Min(1) @Max(200) long size,
                     @RequestParam(required = false) @Size(max = 128) String loginName,
                     @RequestParam(required = false) @Size(max = 20) String outcome) {
        return R.ok(service.page(new LoginAuditQuery(page, size, loginName, outcome)));
    }
}
