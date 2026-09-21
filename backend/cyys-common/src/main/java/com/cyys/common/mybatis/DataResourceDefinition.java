package com.cyys.common.mybatis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 将行级权限所需的表结构声明放在对应持久化模型上。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataResourceDefinition {
    String name() default "";
    String scopeColumn() default "scope_id";
    String departmentColumn() default "org_id";
    String actorColumn() default "created_by";
    String ancestorsColumn() default "";
    String versionColumn() default "version";
    String[] immutableColumns() default {};
}
