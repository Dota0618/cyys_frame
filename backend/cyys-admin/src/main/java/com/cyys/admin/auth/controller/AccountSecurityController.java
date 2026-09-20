package com.cyys.admin.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.admin.auth.dto.ResetPasswordCommand;
import com.cyys.admin.auth.service.PasswordService;
import com.cyys.common.web.PlatformControl;
import com.cyys.common.web.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@PlatformControl
@RequiredArgsConstructor
@RequestMapping("/api/admin/platform/accounts")
public class AccountSecurityController {
    private final PasswordService passwords;

    @PostMapping("/{id}/password")
    @SaCheckPermission("platform:account:reset-password")
    public R<Void> reset(@PathVariable String id,@Valid @RequestBody ResetPasswordCommand command) {
        passwords.reset(id,command); return R.ok();
    }
}
