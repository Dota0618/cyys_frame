package com.cyys.common.mybatis;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import java.util.Set;
import static com.cyys.common.mybatis.DataPolicyRegistry.denied;

/** 对写入形状和绑定值检查；批量由服务事务逐条写，避免插件静默改写部分 ID。 */
final class SqlWriteGuard {
    private SqlWriteGuard() {}

    static void check(Statement sql, MappedStatement mapped, BoundSql bound,
                      DataResource resource) {
        Expression where;
        if (sql instanceof Update update) {
            if (update.getJoins() != null || update.getStartJoins() != null || update.getFromItem() != null) {
                throw denied("普通写入不支持多表 UPDATE");
            }
            for (var set : update.getUpdateSets()) {
                for (Column column : set.getColumns()) {
                    String name = name(column.getColumnName());
                    if (Set.of("id", "created_by", "created_at", resource.scopeColumn()).contains(name)
                            || resource.immutableColumns().contains(name)) throw denied("普通更新不能修改归属或受保护字段");
                }
            }
            where = update.getWhere();
        } else if (sql instanceof Delete delete) {
            if (delete.getJoins() != null || (delete.getTables() != null && !delete.getTables().isEmpty())) {
                throw denied("普通写入不支持多表 DELETE");
            }
            where = delete.getWhere();
        } else {
            throw denied("未支持的写入命令");
        }
        if (!selected(where, "id", mapped, bound)) throw denied("写入必须选择唯一记录，单位条件不算记录选择");
        if (resource.versionColumn() != null && !selected(where, resource.versionColumn(), mapped, bound)) {
            throw denied("写入必须携带当前记录版本");
        }
    }

    private static boolean selected(Expression expression, String column, MappedStatement mapped, BoundSql bound) {
        if (expression instanceof AndExpression and) {
            return selected(and.getLeftExpression(), column, mapped, bound)
                    || selected(and.getRightExpression(), column, mapped, bound);
        }
        if (expression instanceof ExpressionList<?> list && list.size() == 1) {
            return selected(list.getFirst(), column, mapped, bound);
        }
        if (expression instanceof EqualsTo equals && equals.getLeftExpression() instanceof Column field
                && name(field.getColumnName()).equals(column)) {
            Object value = scalar(equals.getRightExpression(), mapped, bound);
            return value != null && !value.toString().isBlank();
        }
        return false;
    }

    private static Object scalar(Expression expression, MappedStatement mapped, BoundSql bound) {
        if (expression instanceof StringValue value) return value.getValue();
        if (expression instanceof LongValue value) return value.getValue();
        if (expression instanceof NullValue) return null;
        if (expression instanceof JdbcParameter parameter && parameter.getIndex() != null) {
            int index = parameter.getIndex() - 1;
            if (index < 0 || index >= bound.getParameterMappings().size()) throw denied("无法解析写入参数");
            String property = bound.getParameterMappings().get(index).getProperty();
            if (bound.hasAdditionalParameter(property)) return bound.getAdditionalParameter(property);
            Object object = bound.getParameterObject();
            if (object == null) return null;
            if (mapped.getConfiguration().getTypeHandlerRegistry().hasTypeHandler(object.getClass())) return object;
            return mapped.getConfiguration().newMetaObject(object).getValue(property);
        }
        throw denied("记录选择或归属值必须是明确的绑定值");
    }

    static String name(String identifier) { return identifier.replace("`", "").toLowerCase(java.util.Locale.ROOT); }
}
