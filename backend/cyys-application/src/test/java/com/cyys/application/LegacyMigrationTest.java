package com.cyys.application;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

/** 同一已授权测试库中的专属表演练；不新建库，不执行 Flyway.clean，不触碰正式表。 */
@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class LegacyMigrationTest extends HttpTestSupport {
    @Autowired DataSource dataSource;
    @Autowired PlatformTransactionManager transactions;
    private static final String PREFIX="g1_mig_";
    private static final List<String> OWNED=List.of("sys_data_audit","sys_role_org","sys_org","sys_role_menu",
            "sys_user_role","sys_user_scope","sys_role","sys_menu","sys_user","sys_scope","history","legacy_org","owner_map");

    @Test
    void explicitLocalBootstrapRunsBetweenV1AndV3WithProvidedPasswordHash() throws Exception {
        withOwnedTables(() -> {
            assertThat(migrate("3",true).migrate().migrationsExecuted).isEqualTo(3);
            String hash=jdbc.queryForObject("select password_hash from g1_mig_sys_user where id='local-test-account'",String.class);
            assertThat(cn.dev33.satoken.secure.BCrypt.checkpw(PASSWORD,hash)).isTrue();
            assertThat(jdbc.queryForObject("select count(*) from g1_mig_sys_user_scope where user_id='local-test-account' and scope_id='local-test-unit' and member_role='OWNER'",Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForList("select code from g1_mig_sys_menu",String.class)).containsExactly("platform:scope:access");
        });
    }

    @Test
    void emptyVersionedSchemaUpgradesAndLeavesNoForeignKeys() throws Exception {
        withOwnedTables(() -> {
            assertThat(migrate("3").migrate().migrationsExecuted).isEqualTo(2);
            assertThat(jdbc.queryForObject("select count(*) from g1_mig_history where success=1 and version in ('1','3')",Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("select count(*) from information_schema.referential_constraints where constraint_schema=database() and table_name like 'g1_mig_%'",Integer.class)).isZero();
        });
    }

    @Test
    void representativeLegacyOrganizationsRequireExplicitOwnershipBeforeUpgrade() throws Exception {
        withOwnedTables(() -> {
            migrate("1").migrate();
            new ResourceDatabasePopulator(new ClassPathResource("legacy-org-sample.sql")).execute(dataSource);
            jdbc.execute("create table g1_mig_owner_map (old_id varchar(32) primary key,scope_id varchar(32) not null)");
            jdbc.update("insert into g1_mig_sys_scope(id,code,name) values('legacy-a','legacy-a','演练单位 A'),('legacy-b','legacy-b','演练单位 B')");
            jdbc.update("insert into g1_mig_owner_map values('b566123f944f11ec99f860f2625779f6','legacy-a'),('b5666780944f11ec99f860f2625779f6','legacy-b'),('b566437c944f11ec99f860f2625779f6','legacy-b')");
            jdbc.update("insert into g1_mig_legacy_org values('g1_unknown','missing','未确认归属','2',0,CURRENT_TIMESTAMP)");
            assertThatThrownBy(this::copyMappedDepartments).isInstanceOf(IllegalStateException.class).hasMessageContaining("未知归属");
            assertThat(jdbc.queryForObject("select count(*) from g1_mig_sys_org",Integer.class)).isZero();
            // 隔离待处理记录，仅定向移除本例注入的反例；正常旧样本全部保留。
            jdbc.update("delete from g1_mig_legacy_org where id='g1_unknown'");
            copyMappedDepartments();
            migrate("3").migrate();
            assertThat(jdbc.queryForList("select scope_id from g1_mig_sys_org order by sort,id",String.class)).containsExactly("legacy-a","legacy-b","legacy-b");
            assertThat(jdbc.queryForObject("select parent_id from g1_mig_sys_org where name='办公室'",String.class)).isEqualTo("b5666780944f11ec99f860f2625779f6");
            assertThat(jdbc.queryForObject("select count(*) from g1_mig_sys_org where parent_id is null and version=0",Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("select count(*) from g1_mig_sys_org d join g1_mig_sys_org p on p.id=d.parent_id where d.scope_id<>p.scope_id",Integer.class)).isZero();
        });
    }

    @Test
    void unconvertedLegacyCustomRangeStopsBeforeForeignKeysAreRemoved() throws Exception {
        withOwnedTables(() -> {
            migrate("1").migrate();
            jdbc.update("insert into g1_mig_sys_role(id,scope_id,code,name,data_range,custom_orgs) values('legacy-role','legacy-a','legacy','legacy',2,'unmapped-department')");
            assertThatThrownBy(() -> migrate("3").migrate()).isInstanceOf(org.flywaydb.core.api.FlywayException.class);
            assertThat(jdbc.queryForObject("select custom_orgs from g1_mig_sys_role where id='legacy-role'",String.class)).isEqualTo("unmapped-department");
            assertThat(jdbc.queryForObject("select count(*) from information_schema.referential_constraints where constraint_schema=database() and table_name like 'g1_mig_%'",Integer.class)).isEqualTo(7);
        });
    }

    @Test
    void versionFourBackfillsLocalNamesWithoutChangingGlobalAccount() throws Exception {
        withOwnedTables(() -> {
            migrate("3",true).migrate();
            jdbc.update("update g1_mig_sys_user set nickname='旧昵称',real_name='旧姓名' where id='local-test-account'");
            migrate("4",true).migrate();
            assertThat(jdbc.queryForObject("select display_name from g1_mig_sys_user_scope where user_id='local-test-account'",String.class)).isEqualTo("旧昵称");
            jdbc.update("update g1_mig_sys_user_scope set display_name='单位专用姓名'");
            assertThat(jdbc.queryForObject("select nickname from g1_mig_sys_user where id='local-test-account'",String.class)).isEqualTo("旧昵称");
            assertThat(jdbc.queryForObject("select version from g1_mig_sys_user_scope where user_id='local-test-account'",Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from information_schema.referential_constraints where constraint_schema=database() and table_name like 'g1_mig_%'",Integer.class)).isZero();
        });
    }

    private void copyMappedDepartments() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            int unknown=jdbc.queryForObject("select count(*) from g1_mig_legacy_org o left join g1_mig_owner_map m on m.old_id=o.id left join g1_mig_sys_scope s on s.id=m.scope_id where s.id is null",Integer.class);
            if (unknown!=0) throw new IllegalStateException("旧组织存在未知归属，停止整个导入批次");
            jdbc.update("insert into g1_mig_sys_org(id,scope_id,parent_id,ancestors,name,sort,created_at) "
                    + "select o.id,m.scope_id,case when o.type='1' then '0' else o.parent_id end,"
                    + "case when o.type='1' then '' else o.parent_id end,o.name,o.sort,o.date_create "
                    + "from g1_mig_legacy_org o join g1_mig_owner_map m on m.old_id=o.id");
        });
    }

    private Flyway migrate(String target) throws Exception {
        return migrate(target,false);
    }

    private Flyway migrate(String target, boolean bootstrap) throws Exception {
        Path directory=Path.of("target","legacy-rehearsal",bootstrap?"bootstrap":"system").toAbsolutePath();
        Files.createDirectories(directory);
        var names=new java.util.ArrayList<>(List.of("V1__init_system_schema.sql","V3__data_isolation_foundation.sql","V4__unit_member_management.sql"));
        if (bootstrap) names.add("V2__local_identity_fixture.sql");
        for (String name : names) {
            String source=Files.readString(Path.of("..","..","migrations",name.startsWith("V2")?"dev-data":"system",name));
            String isolated=source.replaceAll("\\b(sys_[a-z_]+)\\b",PREFIX+"$1")
                    .replaceAll("\\b((?:fk|ck|uk|idx)_[a-z_]+)\\b",PREFIX+"$1");
            Files.writeString(directory.resolve(name),isolated);
        }
        // 仅测试专属历史表从 0 起步；应用的 baseline-on-migrate=false 不变。
        return Flyway.configure().dataSource(dataSource).table(PREFIX+"history").locations("filesystem:"+directory)
                .placeholders(java.util.Map.of("bootstrapScopeId","local-test-unit","bootstrapAccountId","local-test-account",
                        "bootstrapLoginName","local-test-login","bootstrapPasswordHash",PASSWORD_HASH))
                .baselineOnMigrate(true).baselineVersion("0").target(target).cleanDisabled(true).load();
    }

    private void withOwnedTables(Checked task) throws Exception {
        assertThat(jdbc.queryForList("select table_name from information_schema.tables where table_schema=database() and table_name like 'g1_mig_%'",String.class)).as("拒绝覆盖上次或其他任务的残留表").isEmpty();
        try { task.run(); }
        finally {
            for (String table : OWNED) jdbc.execute("drop table if exists "+PREFIX+table);
        }
    }
    private interface Checked { void run() throws Exception; }
}
