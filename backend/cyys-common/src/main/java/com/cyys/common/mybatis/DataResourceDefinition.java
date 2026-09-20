package com.cyys.common.mybatis;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 将行级权限所需的表结构声明放在对应持久化模型上。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataResourceDefinition {
    String name();
    String scopeColumn();
    String departmentColumn();
    String actorColumn();
    String ancestorsColumn() default "";
    String versionColumn() default "";
    String[] immutableColumns() default {};
}
