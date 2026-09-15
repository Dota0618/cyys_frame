package com.cyys.application;

import com.cyys.admin.department.mapper.SysOrgMapper;
import com.cyys.admin.department.model.SysOrg;
import com.cyys.admin.scope.mapper.SysScopeMapper;
import com.cyys.admin.scope.service.ScopeConfigurationValidator;
import com.cyys.common.satoken.DataIsolationProperties;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest(classes = CyysApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DatabaseFoundationTest extends HttpTestSupport {
    @Autowired SysOrgMapper orgMapper;
    @Autowired SysScopeMapper scopeMapper;

    @Test
    void auditUsesVerifiedActorAndPreservesCreationFields() {
        permission("role-a", "g1-create", "department:create", false);
        permission("role-a", "g1-update", "department:update", false);
        membership("u-bob", "scope-a", false);
        grant("u-bob", "role-a", "scope-a");
        jdbc.update("update sys_role set data_range=1 where id='role-a'");
        var org = new SysOrg();
        org.setId("audit-org");
        org.setScopeId("scope-a");
        org.setName("审计验证");
        org.setCreatedBy("forged");
        org.setUpdatedBy("forged");
        ScopeContext.set(new RequestIdentity("u-alice", "scope-a", List.of(), List.of(), List.of()));
        try {
            assertThat(orgMapper.insert(org)).isEqualTo(1);
        } finally {
            ScopeContext.clear();
        }
        assertThat(jdbc.queryForObject("select created_by from sys_org where id='audit-org'", String.class))
                .isEqualTo("u-alice");
        org.setName("已修改");
        org.setCreatedBy("forged-again");
        ScopeContext.set(new RequestIdentity("u-bob", "scope-a", List.of(), List.of(), List.of()));
        try {
            assertThat(orgMapper.updateById(org)).isEqualTo(1);
        } finally {
            ScopeContext.clear();
        }
        assertThat(jdbc.queryForObject("select created_by from sys_org where id='audit-org'", String.class)).isEqualTo("u-alice");
        assertThat(jdbc.queryForObject("select updated_by from sys_org where id='audit-org'", String.class)).isEqualTo("u-bob");
    }

    @Test
    void missingFixedScopeFailsDatabaseBackedStartupValidation() {
        var validator = new ScopeConfigurationValidator(
                new DataIsolationProperties(DataIsolationProperties.Mode.SINGLE, "missing-scope"), scopeMapper);
        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("sys_scope.id");
    }

    @Test
    void flywayMigratesEmptySchemaOnceAndMenuTypeIsConstrained() {
        Integer count = jdbc.queryForObject("select count(*) from flyway_schema_history where version='1' and success=1", Integer.class);
        assertThat(count).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("insert into sys_menu(id,name,code,type) values('bad','bad','bad',4)"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class)
                .hasRootCauseInstanceOf(java.sql.SQLException.class)
                .rootCause().hasMessageContaining("ck_menu_type");
    }
}
