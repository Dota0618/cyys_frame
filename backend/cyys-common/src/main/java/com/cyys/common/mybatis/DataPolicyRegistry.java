package com.cyys.common.mybatis;

import com.cyys.common.web.ApiException;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 没有登记的表、方法或命令类型均失败；控制路径例外精确到语句。 */
public final class DataPolicyRegistry {
    private final Map<String, DataResource> resources;
    private final Map<String, SqlCommandType> controlStatements;

    public DataPolicyRegistry(List<DataResource> resources, Map<String, SqlCommandType> controlStatements) {
        this.resources = resources.stream().collect(Collectors.toUnmodifiableMap(DataResource::name, Function.identity()));
        this.controlStatements = Map.copyOf(controlStatements);
    }

    public boolean controlledStatement(MappedStatement statement) {
        SqlCommandType allowed = controlStatements.get(statement.getId());
        if (allowed != null && allowed != statement.getSqlCommandType()) throw denied("控制路径命令类型不匹配");
        return allowed != null;
    }

    public Policy policy(MappedStatement statement) {
        String id = statement.getId();
        // MP 自动 COUNT 来自原查询；显式自定义 COUNT 必须自行标注。
        if (id.endsWith("_mpCount")) id = id.substring(0, id.length() - "_mpCount".length());
        int split = id.lastIndexOf('.');
        try {
            Class<?> mapper = Class.forName(id.substring(0, split));
            String methodName = id.substring(split + 1);
            for (Method method : mapper.getDeclaredMethods()) {
                if (!method.getName().equals(methodName)) continue;
                DataAccess access = method.getAnnotation(DataAccess.class);
                if (access != null) return checked(access.resource(), access.permission(), access.operation(), access.view(), statement);
            }
            ProtectedMapper annotation = mapper.getAnnotation(ProtectedMapper.class);
            if (annotation != null) {
                DataAccess.Operation operation;
                if (Set.of("selectById", "selectBatchIds", "selectList", "selectCount", "selectMaps", "selectObjs").contains(methodName)) {
                    operation = DataAccess.Operation.READ;
                } else if (methodName.equals("insert")) {
                    operation = DataAccess.Operation.CREATE;
                } else if (methodName.equals("updateById")) {
                    operation = DataAccess.Operation.UPDATE;
                } else {
                    throw denied("未登记的 Mapper 操作");
                }
                return checked(annotation.value(), annotation.value() + ":" + operation.name().toLowerCase(java.util.Locale.ROOT),
                        operation, DataAccess.View.CURRENT_SCOPE, statement);
            }
        } catch (ClassNotFoundException | IndexOutOfBoundsException exception) {
            throw denied("无法识别 Mapper 策略");
        }
        throw denied("Mapper 缺少资源策略");
    }

    public DataResource resource(String name) {
        DataResource resource = resources.get(name);
        if (resource == null) throw denied("未登记的资源");
        return resource;
    }

    private Policy checked(String resourceName, String permission, DataAccess.Operation operation,
                           DataAccess.View view, MappedStatement statement) {
        DataResource resource = resources.get(resourceName);
        if (resource == null || permission == null || !permission.matches("[a-z][a-z0-9:_-]+")) throw denied("未登记的资源或权限");
        SqlCommandType command = statement.getSqlCommandType();
        boolean matches = switch (operation) {
            case READ -> command == SqlCommandType.SELECT;
            case CREATE -> command == SqlCommandType.INSERT || command == SqlCommandType.SELECT;
            case UPDATE -> command == SqlCommandType.UPDATE || command == SqlCommandType.SELECT;
            case DELETE -> command == SqlCommandType.DELETE || command == SqlCommandType.UPDATE || command == SqlCommandType.SELECT;
        };
        if (!matches || (view == DataAccess.View.AUTHORIZED_SCOPES && operation != DataAccess.Operation.READ)) {
            throw denied("SQL 命令与授权策略不一致");
        }
        return new Policy(resource, permission, operation, view);
    }

    public static ApiException denied(String message) { return new ApiException(403, message); }

    public record Policy(DataResource resource, String permission, DataAccess.Operation operation, DataAccess.View view) {}
}
