package com.cyys.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginCmd(
        @NotBlank(message = "登录名不能为空") @Size(max = 64) String loginName,
        @NotBlank(message = "密码不能为空") @Size(max = 72) String password,
        @Size(max = 32) String scopeId) {
}
