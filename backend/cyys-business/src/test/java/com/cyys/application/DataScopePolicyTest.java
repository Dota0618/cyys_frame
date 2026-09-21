package com.cyys.application;

import com.cyys.admin.department.model.SysOrg;
import com.cyys.admin.notice.model.OrgNotice;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.auth.service.RoleRowPolicyProvider;
import com.cyys.admin.user.model.SysUserScope;
import com.cyys.common.mybatis.*;
import com.cyys.common.satoken.*;
import com.cyys.common.web.ApiException;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataScopePolicyTest {
    private final Configuration configuration = new Configuration();
    private final DataPolicyRegistry registry = new DataPolicyRegistry();
    private final RowPolicyProvider provider = mock(RowPolicyProvider.class);
    private final GuardedDataPermissionInterceptor interceptor = new GuardedDataPermissionInterceptor(registry, provider);

    @BeforeEach void identity() {
        configuration.addMapper(NoticeMapper.class); configuration.addMapper(DepartmentMapper.class);
        ScopeContext.set(new RequestIdentity("actor", "a", List.of(), List.of()));
        DataPermissionContext.set(DataPolicyRegistry.View.CURRENT_SCOPE);
        when(provider.predicate(any(), any(), anyString())).thenAnswer(call -> call.getArgument(2) + ".scope_id = 'a'");
    }
    @AfterEach void clear() {
        ScopeContext.clear(); DataPermissionContext.clear(); TransactionSynchronizationManager.clear();
    }

    @Test void registeredJoinTablesAreIndividuallyFilteredWithAliases() {
        String sql = rewrite("select n.id,d.name from sys_org_notice n left join sys_org d on d.id=n.org_id where n.deleted=0");
        assertThat(sql).contains("n.scope_id = 'a'", "d.scope_id = 'a'");
        verify(provider).predicate(argThat(p -> p.resource().table().equals("sys_org_notice")), any(), eq("n"));
        verify(provider).predicate(argThat(p -> p.resource().table().equals("sys_org")), any(), eq("d"));
    }

    @Test void countAndUnionAreFilteredAndMissingContextFailsClosed() {
        assertThat(rewrite("select count(*) from sys_org_notice n")).contains("n.scope_id = 'a'");
        assertThat(rewrite("select n.id from sys_org_notice n union all select m.id from sys_org_notice m"))
                .contains("n.scope_id = 'a'", "m.scope_id = 'a'");
        DataPermissionContext.clear();
        assertThatThrownBy(() -> rewrite("select * from sys_org_notice"))
                .isInstanceOf(ApiException.class).hasMessageContaining("缺少数据范围");
    }

    @Test void unknownTablesAndUncoveredSubqueriesAreRejected() {
        assertThatThrownBy(() -> rewrite("select n.id from sys_org_notice n join unknown_table x on x.id=n.id"))
                .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> rewrite("select case when 1=1 then (select count(*) from sys_org_notice s) else 0 end from sys_org_notice n"))
                .isInstanceOf(ApiException.class).hasMessageContaining("未受完整保护");
    }

    @Test void actualSqlDeterminesAccessAndReadOnlyViewsCannotWrite() {
        var update = statement("update sys_org_notice set title='new' where id='1' and version=0", SqlCommandType.UPDATE);
        assertThat(registry.policy(update).access()).isEqualTo(DataPolicyRegistry.Access.WRITE);
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
        assertThatThrownBy(() -> registry.policy(update)).hasMessageContaining("只读事务");
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);
        DataPermissionContext.clear(); DataPermissionContext.set(DataPolicyRegistry.View.AUTHORIZED_SCOPES);
        assertThatThrownBy(() -> registry.policy(update)).hasMessageContaining("只允许读取");
    }

    @Test void userScopePolicyDoesNotQueryOrEmbedActionPermissions() {
        var lookups = mock(DataAuthorizationMapper.class);
        @SuppressWarnings("unchecked") ObjectProvider<DataAuthorizationMapper> source = mock(ObjectProvider.class);
        when(source.getObject()).thenReturn(lookups);
        when(lookups.activeProfile("actor")).thenReturn(new SysUserScope());
        when(lookups.hasInstitutionAccess("actor", "a")).thenReturn(true);
        var scopes = new RoleRowPolicyProvider(source);
        var policy = registry.policy(statement("select * from sys_org_notice", SqlCommandType.SELECT));
        scopes.authorize(policy, ScopeContext.get());
        String predicate = scopes.predicate(policy, ScopeContext.get(), "n");
        assertThat(predicate).contains("n.scope_id='a'", "p.data_range=0", "p.data_range=5", "p.data_range=6")
                .doesNotContain("sys_role", "sys_menu", "pol_m.code");
        verify(lookups, never()).hasPermission(any(), any());
    }

    private String rewrite(String sql) {
        var mapped = statement(sql, SqlCommandType.SELECT);
        var bound = mapped.getBoundSql(null);
        interceptor.beforeQuery(null, mapped, null, RowBounds.DEFAULT, null, bound);
        return bound.getSql();
    }
    private MappedStatement statement(String sql, SqlCommandType type) {
        return new MappedStatement.Builder(configuration, NoticeMapper.class.getName() + ".query", new StaticSqlSource(configuration, sql), type).build();
    }
    @ProtectedMapper(OrgNotice.class) interface NoticeMapper {}
    @ProtectedMapper(SysOrg.class) interface DepartmentMapper {}
}
