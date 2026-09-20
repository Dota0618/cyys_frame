package com.cyys.admin.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import com.cyys.common.web.ApiException;
import java.nio.charset.StandardCharsets;

/** 新建账号、改密与重置共用的密码规则；不改变存量账号登录校验。 */
public final class PasswordPolicy {
    private PasswordPolicy() {}

    public static void validateNew(String password) {
        if(password==null || password.isBlank() || password.length()<12) throw new ApiException(400,"新密码至少 12 位，不能全为空白");
        validateBytes(password);
    }

    public static void validateBytes(String password) {
        if(password==null || password.getBytes(StandardCharsets.UTF_8).length>72) throw new ApiException(400,"密码不能超过 72 个 UTF-8 字节");
    }

    public static String hash(String password) {
        validateNew(password);
        return BCrypt.hashpw(password);
    }
}
