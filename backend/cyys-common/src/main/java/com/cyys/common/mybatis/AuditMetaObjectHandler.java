package com.cyys.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.cyys.common.satoken.ScopeContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        var identity = ScopeContext.get();
        String actor = identity.loginId();
        if (metaObject.hasSetter("scopeId")) {
            Object supplied = getFieldValByName("scopeId", metaObject);
            String scope = identity.requireScopeId();
            if (supplied != null && !scope.equals(supplied)) throw DataPolicyRegistry.denied("新增不能指定其他单位归属");
            setFieldValByName("scopeId", scope, metaObject);
        }
        LocalDateTime now = LocalDateTime.now();
        // 审计字段由服务端覆盖，不能保留调用方传入的创建人。
        setFieldValByName("createdBy", actor, metaObject);
        setFieldValByName("createdAt", now, metaObject);
        setFieldValByName("updatedBy", actor, metaObject);
        setFieldValByName("updatedAt", now, metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("updatedBy", ScopeContext.get().loginId(), metaObject);
        setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
    }
}
