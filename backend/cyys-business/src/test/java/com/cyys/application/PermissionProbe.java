package com.cyys.application;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 仅在测试类路径验证真实 Sa-Token 注解与上下文，不进入应用 JAR。 */
@RestController
public class PermissionProbe {
    @GetMapping("/api/test/permission")
    @SaCheckPermission("test:read")
    public R<RequestIdentity> permission() {
        return R.ok(ScopeContext.get());
    }
}
