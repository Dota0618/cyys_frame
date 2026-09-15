package com.cyys.common.mybatis;

import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import static com.cyys.common.mybatis.DataPolicyRegistry.denied;

/** 使用 MP DataPermissionInterceptor 改写，并核对每个原始数据源确实进入行策略。 */
public final class GuardedDataPermissionInterceptor extends DataPermissionInterceptor {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GuardedDataPermissionInterceptor.class);
    private final DataPolicyRegistry registry;
    private final RowPolicyProvider provider;

    public GuardedDataPermissionInterceptor(DataPolicyRegistry registry, RowPolicyProvider provider) {
        this.registry = registry;
        this.provider = provider;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement mapped, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql bound) {
        rewrite(mapped, bound);
    }

    @Override
    public void beforePrepare(StatementHandler handler, Connection connection, Integer timeout) {
        var statement = PluginUtils.mpStatementHandler(handler);
        if (statement.mappedStatement().getSqlCommandType() != SqlCommandType.SELECT) {
            rewrite(statement.mappedStatement(), statement.boundSql());
        }
    }

    private void rewrite(MappedStatement mapped, BoundSql bound) {
        if (InterceptorIgnoreHelper.willIgnoreDataPermission(mapped.getId())) throw denied("禁止忽略数据隔离策略");
        if (registry.controlledStatement(mapped)) return;
        if (mapped.getStatementType() != org.apache.ibatis.mapping.StatementType.PREPARED) throw denied("业务不支持原生或存储过程语句");
        var policy = registry.policy(mapped);
        var identity = ScopeContext.get();
        DataTransactionGuard.bind(identity, policy.view());
        provider.authorize(policy, identity);
        // 普通新增由 DTO、写服务和 MetaObjectHandler 保证字段，不再逐列解析 INSERT。
        if (mapped.getSqlCommandType() == SqlCommandType.INSERT) {
            if (!mapped.getId().endsWith(".insert")) throw denied("自定义新增须通过明确的业务写入路径");
            return;
        }
        try {
            Statement sql = CCJSqlParserUtil.parse(bound.getSql());
            Map<String, Integer> expected = new HashMap<>();
            var sources = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Table, Boolean>());
            new TablesNamesFinder<Void>() {
                @Override public <S> Void visit(Table table, S context) {
                    checkTable(table, policy.resource());
                    // JSQLParser 对 JOIN 的同一个 Table 节点可能回访；按 AST 节点计数。
                    if (sources.add(table)) expected.merge(table.toString(), 1, Integer::sum);
                    return super.visit(table, context);
                }
            }.getTables(sql);
            if (expected.isEmpty()) throw denied("受保护查询缺少数据源");
            if (mapped.getSqlCommandType() != SqlCommandType.SELECT) {
                SqlWriteGuard.check(sql, mapped, bound, policy.resource());
            } else if (!(sql instanceof Select)) {
                throw denied("未支持的查询形状");
            }
            Map<String, Integer> visited = new HashMap<>();
            var rewriter = new DataPermissionInterceptor((MultiDataPermissionHandler) (table, where, statementId) -> {
                checkTable(table, policy.resource());
                visited.merge(table.toString(), 1, Integer::sum);
                String qualifier = table.getAlias() == null ? table.getName() : table.getAlias().getName();
                if (!qualifier.matches("`?[A-Za-z_][A-Za-z0-9_]*`?")) throw denied("不支持的数据源别名");
                try {
                    return CCJSqlParserUtil.parseCondExpression(provider.predicate(policy, identity, qualifier));
                } catch (Exception exception) {
                    throw denied("无法构建行级授权条件");
                }
            });
            String rewritten = rewriter.parserSingle(bound.getSql(), mapped.getId());
            // HAVING/CASE/ON 等未被当前插件遍历的子查询不得漏过滤后执行。
            if (!expected.equals(visited)) {
                LOG.warn("Uncovered data sources: statement={}, expected={}, protected={}", mapped.getId(), expected, visited);
                throw denied("SQL 包含未受完整保护的数据源，请改写查询");
            }
            PluginUtils.mpBoundSql(bound).sql(rewritten);
        } catch (ApiException exception) {
            LOG.warn("Data access rejected: actor={}, scope={}, resource={}, view={}, operation={}, statement={}",
                    identity.loginId(), identity.scopeId(), policy.resource().name(), policy.view(), policy.operation(), mapped.getId());
            throw exception;
        } catch (Exception exception) {
            throw denied("受保护 SQL 无法安全解析");
        }
    }

    private void checkTable(Table table, DataResource resource) {
        if (table.getSchemaName() != null || !SqlWriteGuard.name(table.getName()).equals(resource.table())) {
            throw denied("SQL 引用了未登记或不属于本资源的数据源");
        }
    }
}
