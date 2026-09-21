package com.cyys.application;

import com.cyys.admin.serial.service.SerialNumberService;
import com.cyys.common.mybatis.DataPermissionContext;
import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.satoken.ScopeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"cyys.data-isolation.mode=multi","cyys.data-isolation.single-scope-id=",
                "cyys.serial-numbers.definitions.order.prefix=ORD-", "cyys.serial-numbers.definitions.order.period=DAY",
                "cyys.serial-numbers.definitions.order.width=3", "cyys.serial-numbers.definitions.order.zone=Asia/Shanghai",
                "cyys.serial-numbers.definitions.order.permission=user:create",
                "cyys.serial-numbers.definitions.ticket.prefix=TKT-", "cyys.serial-numbers.definitions.ticket.period=NONE",
                "cyys.serial-numbers.definitions.ticket.width=2", "cyys.serial-numbers.definitions.ticket.zone=Asia/Shanghai",
                "cyys.serial-numbers.definitions.ticket.permission=user:create"})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class SerialNumberIntegrationTest extends IsolationTestSupport {
    @Autowired SerialNumberService numbers;
    @Autowired ApplicationContext context;
    @Autowired PlatformTransactionManager transactions;
    @Autowired org.springframework.core.env.Environment environment;

    @BeforeEach void serialPermissions() {
        allow("role-a", "user:create", false);
        allow("role-b", "user:create", false);
    }

    @AfterEach void serialCleanup() {
        DataPermissionContext.clear();
        ScopeContext.clear();
        jdbc.update("delete from sys_serial_counter where scope_id in ('scope-a','scope-b') and business_code in ('order','ticket')");
        jdbc.update("delete from sys_serial_rule where scope_id in ('scope-a','scope-b') and business_code in ('order','ticket')");
    }

    @Test void separateServiceInstancesAllocateUniqueNumbersAndScopesIndependently() throws Exception {
        // 独立构造的 Spring 事务代理，不能依赖某个 Service 实例上的 synchronized 保证唯一。
        var other=context.getAutowireCapableBeanFactory().createBean(SerialNumberService.class);
        assertThat(other).isNotSameAs(numbers);
        var results=new HashSet<String>();
        try(var workers=Executors.newFixedThreadPool(6)) {
            var tasks=new ArrayList<Future<String>>();
            for(int i=0;i<24;i++) {
                var service=i%2==0?numbers:other;
                tasks.add(workers.submit(() -> next(service,"u-alice","scope-a","ticket")));
            }
            for(var task:tasks) results.add(task.get());
        }
        assertThat(results).hasSize(24).contains("TKT-01","TKT-24");
        assertThat(next(numbers,"u-bob","scope-b","ticket")).isEqualTo("TKT-01");
        assertThat(next(numbers,"u-alice","scope-a","order")).endsWith("-001").startsWith("ORD-");
        assertThat(count("select count(*) from sys_data_audit where scope_id='scope-a' and resource='serial' and operation='RESERVE'"))
                .isEqualTo(25);
    }

    @Test void reservationSurvivesBusinessRollbackAndOldPeriodsDoNotResetCurrentCounter() {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            assertThat(next(numbers,"u-alice","scope-a","ticket")).isEqualTo("TKT-01");
            status.setRollbackOnly();
        });
        assertThat(next(numbers,"u-alice","scope-a","ticket")).isEqualTo("TKT-02");
        jdbc.update("insert into sys_serial_counter(id,scope_id,business_code,period_key,sequence_value,updated_by) values('serial-old','scope-a','order','20000101',998,'u-alice')");
        assertThat(next(numbers,"u-alice","scope-a","order")).endsWith("-001");
        assertThat(next(numbers,"u-alice","scope-a","order")).endsWith("-002");
        assertThat(jdbc.queryForObject("select sequence_value from sys_serial_counter where id='serial-old'",Long.class)).isEqualTo(998L);
    }

    @Test void independentJvmProcessesUseTheSameDatabaseSequenceWithoutDuplicates() throws Exception {
        var directory=java.nio.file.Files.createTempDirectory(java.nio.file.Path.of("target").toAbsolutePath(),"serial-process-");
        java.nio.file.Files.writeString(directory.resolve("fixture-owned"),"SerialNumberIntegrationTest");
        var processes=new ArrayList<Process>();
        try {
            for(String worker:java.util.List.of("a","b")) {
                var arguments=directory.resolve(worker+".args");
                String classpath=System.getProperty("java.class.path").replace('\\','/');
                java.nio.file.Files.write(arguments,java.util.List.of("-cp",'"'+classpath+'"',
                        SerialNumberProcessWorker.class.getName(),'"'+directory.toString().replace('\\','/')+'"',worker));
                String executable=System.getProperty("os.name").startsWith("Windows")?"java.exe":"java";
                var builder=new ProcessBuilder(java.nio.file.Path.of(System.getProperty("java.home"),"bin",executable).toString(),"@"+arguments)
                        .redirectErrorStream(true).redirectOutput(directory.resolve(worker+".log").toFile());
                for(String[] setting:new String[][]{{"CYYS_DB_URL","spring.datasource.url"},{"CYYS_DB_USERNAME","spring.datasource.username"},
                        {"CYYS_DB_PASSWORD","spring.datasource.password"},{"CYYS_REDIS_HOST","spring.data.redis.host"},
                        {"CYYS_REDIS_PORT","spring.data.redis.port"},{"CYYS_REDIS_PASSWORD","spring.data.redis.password"}}) {
                    builder.environment().put(setting[0],environment.getRequiredProperty(setting[1]));
                }
                builder.environment().put("CYYS_ENV","serial-process-test");
                processes.add(builder.start());
            }
            var deadline=java.time.Instant.now().plusSeconds(45);
            while(!java.nio.file.Files.exists(directory.resolve("a.ready")) || !java.nio.file.Files.exists(directory.resolve("b.ready"))) {
                assertThat(processes).as("child startup logs: %s",directory).allMatch(Process::isAlive);
                assertThat(java.time.Instant.now()).as("child startup timeout: %s",directory).isBefore(deadline);
                Thread.sleep(50);
            }
            assertThat(java.nio.file.Files.readString(directory.resolve("a.ready"))).isNotEqualTo(java.nio.file.Files.readString(directory.resolve("b.ready")));
            java.nio.file.Files.writeString(directory.resolve("go"),"allocate concurrently");
            for(var process:processes) {
                assertThat(process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                assertThat(process.exitValue()).as("child result logs: %s",directory).isZero();
            }
            var results=new ArrayList<String>(java.nio.file.Files.readAllLines(directory.resolve("a.results")));
            results.addAll(java.nio.file.Files.readAllLines(directory.resolve("b.results")));
            assertThat(results.stream().filter(value->value.startsWith("A:")).toList()).hasSize(48).doesNotHaveDuplicates().contains("A:TKT-01","A:TKT-48");
            assertThat(results.stream().filter(value->value.startsWith("B:")).toList()).containsExactlyInAnyOrder("B:TKT-01","B:TKT-02");
            assertThat(results.stream().filter(value->value.startsWith("ORDER:")).toList()).hasSize(2).doesNotHaveDuplicates();
            assertThat(count("select count(*) from sys_data_audit where resource='serial' and operation='RESERVE' and scope_id in ('scope-a','scope-b')")).isEqualTo(52);
        } finally {
            for(var process:processes) if(process.isAlive()) { process.destroyForcibly(); process.waitFor(10,java.util.concurrent.TimeUnit.SECONDS); }
        }
    }

    @Test void exhaustedNumbersAndHistoricalRuleChangesNeverRestartSequence() {
        String first=next(numbers,"u-alice","scope-a","order");
        String period=first.substring(4,12);
        jdbc.update("update sys_serial_counter set sequence_value=999 where scope_id='scope-a' and business_code='order' and period_key=?",period);
        assertThatThrownBy(() -> next(numbers,"u-alice","scope-a","order")).hasMessageContaining("耗尽");
        assertThat(count("select count(*) from sys_data_audit where scope_id='scope-a' and resource='serial' and operation='RESERVE'"))
                .isEqualTo(1);
        jdbc.update("update sys_serial_rule set width=4 where scope_id='scope-a' and business_code='order'");
        assertThatThrownBy(() -> next(numbers,"u-alice","scope-a","order")).hasMessageContaining("不能直接修改");
        assertThatThrownBy(() -> next(numbers,"u-alice","scope-a","unknown")).hasMessageContaining("未配置");
    }

    @Test void unauthorizedUnitRevocationAndMissingOperationCannotAllocate() {
        assertThatThrownBy(() -> next(numbers,"u-alice","scope-b","ticket")).hasMessageContaining("有效授权");
        jdbc.update("update sys_scope set status=0 where id='scope-a'");
        assertThatThrownBy(() -> next(numbers,"u-alice","scope-a","ticket")).isInstanceOf(RuntimeException.class);
        jdbc.update("update sys_scope set status=1 where id='scope-a'");
        as("u-alice","scope-a");
        try { assertThatThrownBy(() -> numbers.next("ticket")).hasMessageContaining("缺少功能权限"); }
        finally { ScopeContext.clear(); }
        jdbc.update("update sys_role_menu rm join sys_menu m on m.id=rm.menu_id set rm.deleted=1 where rm.role_id='role-a' and m.code='user:create'");
        assertThatThrownBy(() -> next(numbers,"u-alice","scope-a","ticket")).hasMessageContaining("有效授权");
        assertThat(count("select count(*) from sys_serial_counter where scope_id in ('scope-a','scope-b')")).isZero();
    }

    private String next(SerialNumberService service,String user,String scope,String code) {
        as(user,scope);
        DataPermissionContext.set(DataPolicyRegistry.View.CURRENT_SCOPE);
        try { return service.next(code); }
        finally { DataPermissionContext.clear(); ScopeContext.clear(); }
    }
}
