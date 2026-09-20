package com.cyys.common.mybatis;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cyys.common.web.ApiException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 从 Mapper 类和当前接口上下文生成行级策略；不维护方法名或 SQL 类型清单。 */
@Component
public final class DataPolicyRegistry {
    private final Map<Class<?>, DataResource> resources = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> names = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> tables = new ConcurrentHashMap<>();

    /** 返回 null 表示该 Mapper 已声明由业务 SQL 自行限定数据范围。 */
    public Policy policy(MappedStatement statement) {
        Class<?> mapper = mapperType(statement);
        ProtectedMapper automatic = mapper.getAnnotation(ProtectedMapper.class);
        boolean manual = mapper.isAnnotationPresent(ManualDataScope.class);
        if ((automatic == null && !manual) || (automatic != null && manual)) {
            throw denied("Mapper 必须且只能声明一种数据范围边界");
        }
        if (manual) return null;

        DataPermissionContext.Current current = DataPermissionContext.get();
        Access access = current.access();
        SqlCommandType command = statement.getSqlCommandType();
        if (access == Access.READ && command != SqlCommandType.SELECT) {
            throw denied("只读功能权限不能执行写入 SQL");
        }
        if (current.view() == View.AUTHORIZED_SCOPES && access != Access.READ) {
            throw denied("跨单位视图只允许读取");
        }
        return new Policy(resource(automatic.value()), current.permission(), access, current.view());
    }

    public DataResource resource(Class<?> type) {
        return resources.computeIfAbsent(type, this::createResource);
    }

    private DataResource createResource(Class<?> type) {
        DataResourceDefinition definition = type.getAnnotation(DataResourceDefinition.class);
        TableName tableName = type.getAnnotation(TableName.class);
        if (definition == null || tableName == null) {
            throw denied("数据资源模型缺少定义或表名");
        }
        DataResource resource;
        try {
            resource = new DataResource(definition.name(), tableName.value(), definition.scopeColumn(),
                    definition.departmentColumn(), blankToNull(definition.ancestorsColumn()), definition.actorColumn(),
                    blankToNull(definition.versionColumn()), Set.of(definition.immutableColumns()));
        } catch (IllegalArgumentException exception) {
            throw denied("数据资源字段定义无效");
        }
        unique(names, resource.name(), type, "资源名称重复");
        unique(tables, resource.table(), type, "资源表重复");
        return resource;
    }

    private static Class<?> mapperType(MappedStatement statement) {
        String id = statement.getId();
        if (id.endsWith("_mpCount")) id = id.substring(0, id.length() - "_mpCount".length());
        int split = id.lastIndexOf('.');
        try {
            return Class.forName(id.substring(0, split));
        } catch (ClassNotFoundException | IndexOutOfBoundsException exception) {
            throw denied("无法识别 Mapper 数据范围边界");
        }
    }

    private static void unique(Map<String, Class<?>> owners, String key, Class<?> type, String message) {
        Class<?> existing = owners.putIfAbsent(key, type);
        if (existing != null && existing != type) throw denied(message);
    }

    private static String blankToNull(String value) {
        return value.isBlank() ? null : value;
    }

    public static ApiException denied(String message) {
        return new ApiException(403, message);
    }

    public enum Access { READ, WRITE }
    public enum View { CURRENT_SCOPE, AUTHORIZED_SCOPES }
    public record Policy(DataResource resource, String permission, Access access, View view) {}
}
