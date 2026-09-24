package com.cyys.admin.auth.service;

import cn.dev33.satoken.secure.BCrypt;
import com.cyys.common.web.ApiException;
import java.nio.charset.StandardCharsets;

/** 明文密码的基础校验；密码强度组合要求由具体业务入口决定。 */
public final class PasswordPolicy {
    public static final int MIN_LENGTH = 8;

    private PasswordPolicy() {}

    public static void validateNew(String password) {
        validateNew(password, 0);
    }

    public static void validateNew(String password, int strength) {
        if(password==null || password.isBlank() || password.length()<MIN_LENGTH) throw new ApiException(400,"新密码至少 8 位，不能全为空白");
        validateBytes(password);
        if (strength >= 1 && !hasLetterAndNumber(password))
            throw new ApiException(400, "新密码必须包含字母和数字");
        if (strength >= 2 && (!hasUppercase(password) || !hasLowercase(password) || !hasNumber(password)))
            throw new ApiException(400, "新密码必须包含大写字母、小写字母和数字");
    }

    public static void validateBytes(String password) {
        if(password==null || password.getBytes(StandardCharsets.UTF_8).length>72) throw new ApiException(400,"密码不能超过 72 个 UTF-8 字节");
    }

    public static String hash(String password) {
        validateNew(password);
        return BCrypt.hashpw(password);
    }

    public static String hash(String password, int strength) {
        validateNew(password, strength);
        return BCrypt.hashpw(password);
    }

    private static boolean hasLetterAndNumber(String password) {
        return hasLetter(password) && hasNumber(password);
    }

    private static boolean hasLetter(String password) {
        return password.codePoints().anyMatch(Character::isLetter);
    }

    private static boolean hasNumber(String password) {
        return password.codePoints().anyMatch(Character::isDigit);
    }

    private static boolean hasUppercase(String password) {
        return password.codePoints().anyMatch(Character::isUpperCase);
    }

    private static boolean hasLowercase(String password) {
        return password.codePoints().anyMatch(Character::isLowerCase);
    }
}
