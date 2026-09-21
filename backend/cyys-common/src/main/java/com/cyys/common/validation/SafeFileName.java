package com.cyys.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.springframework.web.multipart.MultipartFile;

import java.lang.annotation.*;

/** 文件显示名称校验，供 JSON 命令和 multipart 上传入口共用。 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT,
        ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {SafeFileName.TextValidator.class, SafeFileName.UploadValidator.class})
public @interface SafeFileName {
    String message() default "文件名不能为空、不能超过200字，且不能包含路径或控制字符";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class TextValidator implements ConstraintValidator<SafeFileName, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return validName(value);
        }

        static boolean validName(String value) {
            if (value == null || value.isBlank() || value.length() > 200) return false;
            String name = value.strip();
            return !name.equals(".") && !name.equals("..") && value.indexOf('/') < 0
                    && value.indexOf('\\') < 0 && value.codePoints().noneMatch(Character::isISOControl);
        }
    }

    class UploadValidator implements ConstraintValidator<SafeFileName, MultipartFile> {
        @Override
        public boolean isValid(MultipartFile value, ConstraintValidatorContext context) {
            return value != null && TextValidator.validName(value.getOriginalFilename());
        }
    }
}
