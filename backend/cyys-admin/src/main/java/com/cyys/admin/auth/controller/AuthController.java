package com.cyys.admin.auth.controller;

import com.cyys.admin.auth.dto.LoginCmd;
import com.cyys.admin.auth.dto.LoginVO;
import com.cyys.admin.auth.service.AuthService;
import com.cyys.common.web.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public R<LoginVO> login(@Valid @RequestBody LoginCmd command, HttpServletRequest request) {
        // 默认只记录直连地址；未建立可信代理边界前不读取客户端转发头。
        return R.ok(authService.login(command, request.getRemoteAddr()));
    }

    @PostMapping("/logout")
    public R<Void> logout() {
        authService.logout();
        return R.ok();
    }

    @GetMapping("/me")
    public R<LoginVO.UserInfo> me() {
        return R.ok(authService.me());
    }
}
