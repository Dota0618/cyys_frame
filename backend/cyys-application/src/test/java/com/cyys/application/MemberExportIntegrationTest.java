package com.cyys.application;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"cyys.data-isolation.mode=multi","cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
@Import(MemberExportIntegrationTest.ExportBarrierConfiguration.class)
class MemberExportIntegrationTest extends IsolationTestSupport {
    @Autowired ExportBarrier barrier;
    @BeforeEach void exportPermissions() {
        allow("role-a","user:export",false); allow("role-b","user:export",false);
        membership("u-bob","scope-a",false);
        jdbc.update("update sys_user_scope set display_name='=1+1' where user_id='u-alice'");
        jdbc.update("update sys_user_scope set display_name='乙成员',org_id='g1-a-child' where user_id='u-bob' and scope_id='scope-a'");
        jdbc.update("update sys_user_scope set display_name='另一个单位秘密' where user_id='u-bob' and scope_id='scope-b'");
    }

    @Test void exportUsesItsOwnUnitAndRoleRangeAndNeverSerializesAccountSecrets() throws Exception {
        String alice=token("alice");
        var all=export(alice,"scope-a","{}");
        assertThat(all.statusCode()).isEqualTo(200);
        var rows=rows(all);
        assertThat(rows).hasSize(3);
        assertThat(rows.toString()).contains("=1+1","乙成员","alice","bob").doesNotContain("另一个单位秘密","private-id-card",PASSWORD_HASH);
        assertThat(export(alice,"scope-b","{}").statusCode()).isEqualTo(403);
        jdbc.update("update sys_user_scope set data_range=5 where user_id='u-alice' and scope_id='scope-a'");
        rows=rows(export(alice,"scope-a","{}"));
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1)).contains("=1+1","alice","不可见或已失效").doesNotContain("bob");
        jdbc.update("update sys_user_scope set data_range=3 where user_id='u-alice' and scope_id='scope-a'");
        assertThat(rows(export(alice,"scope-a","{}"))).hasSize(2);
        jdbc.update("update sys_user_scope set data_range=4 where user_id='u-alice' and scope_id='scope-a'");
        assertThat(rows(export(alice,"scope-a","{}"))).hasSize(3);
        jdbc.update("update sys_user_scope set data_range=2 where user_id='u-alice' and scope_id='scope-a'");
        jdbc.update("insert into sys_user_scope_org(member_id,scope_id,org_id) values('u-alice-scope-a','scope-a','g1-a-child')");
        rows=rows(export(alice,"scope-a","{}"));
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1)).contains("bob").doesNotContain("alice");
    }

    @Test void filtersAndEmptyResultsWorkAndReadPermissionCannotReplaceRevokedExport() throws Exception {
        String alice=token("alice");
        assertThat(rows(export(alice,"scope-a","{\"name\":\"乙成员\"}"))).hasSize(2);
        assertThat(rows(export(alice,"scope-a","{\"name\":\"不存在\"}"))).hasSize(1);
        allow("role-a","user:read",false);
        jdbc.update("update sys_role_menu rm join sys_menu m on m.id=rm.menu_id set rm.deleted=1 where rm.role_id='role-a' and m.code='user:export'");
        assertThat(request("GET","/api/admin/users",alice,"scope-a",null).status()).isEqualTo(200);
        assertThat(export(alice,"scope-a","{}").statusCode()).isEqualTo(403);
        assertThat(export(alice,"scope-a","{\"scopeId\":\"scope-b\"}").statusCode()).isIn(400,403);
    }

    @Test void platformReadAllDoesNotGrantExportUntilTargetUnitOperationIsGranted() throws Exception {
        platformUser();
        allow("role-platform","user:export",false);
        allow("role-platform","platform:data:read-all",true);
        String admin=token("platform");
        assertThat(export(admin,"scope-a","{}").statusCode()).isEqualTo(403);
        allow("role-platform","platform:data:operate",true);
        assertThat(rows(export(admin,"scope-a","{}"))).hasSize(3);
        var b=rows(export(admin,"scope-b","{}"));
        assertThat(b).hasSize(2);
        assertThat(b.get(1)).contains("另一个单位秘密").doesNotContain("alice");
    }

    @Test void revocationAfterRowsWereLoadedNeverReturnsThePreviouslyAuthorizedWorkbook() throws Exception {
        String alice=token("alice");
        try(var worker=Executors.newSingleThreadExecutor()) {
            for(boolean removePermission : List.of(true,false)) {
                jdbc.update("update sys_role_menu rm join sys_menu m on m.id=rm.menu_id set rm.deleted=0 where rm.role_id='role-a' and m.code='user:export'");
                jdbc.update("update sys_user_scope set data_range=1 where user_id='u-alice' and scope_id='scope-a'");
                var gate=new ExportGate();
                barrier.pending.set(gate);
                var response=worker.submit(() -> export(alice,"scope-a","{}"));
                try {
                    assertThat(gate.loaded.await(10,TimeUnit.SECONDS)).as("real export SQL completed").isTrue();
                    if(removePermission) {
                        jdbc.update("update sys_role_menu rm join sys_menu m on m.id=rm.menu_id set rm.deleted=1 where rm.role_id='role-a' and m.code='user:export'");
                    } else jdbc.update("update sys_user_scope set data_range=5 where user_id='u-alice' and scope_id='scope-a'");
                } finally { gate.resume.countDown(); barrier.pending.set(null); }
                var denied=response.get(20,TimeUnit.SECONDS);
                assertThat(denied.statusCode()).isEqualTo(removePermission?403:409);
                assertThat(denied.headers().firstValue("Content-Type").orElse("")).startsWith("application/json");
                assertThat(count("select count(*) from sys_data_audit where operation='EXPORT_GENERATED' and scope_id='scope-a'")).isZero();
            }
        }
    }

    static final class ExportGate {
        final CountDownLatch loaded=new CountDownLatch(1);
        final CountDownLatch resume=new CountDownLatch(1);
    }
    @TestConfiguration(proxyBeanMethods=false)
    static class ExportBarrierConfiguration {
        @Bean ExportBarrier exportBarrier() { return new ExportBarrier(); }
    }
    /** 仅在测试类路径暂停真实查询结果，不替换 SQL、授权或生成的内容。 */
    @Intercepts({
        @Signature(type=Executor.class,method="query",args={MappedStatement.class,Object.class,RowBounds.class,ResultHandler.class}),
        @Signature(type=Executor.class,method="query",args={MappedStatement.class,Object.class,RowBounds.class,ResultHandler.class,org.apache.ibatis.cache.CacheKey.class,org.apache.ibatis.mapping.BoundSql.class})
    })
    static class ExportBarrier implements Interceptor {
        final AtomicReference<ExportGate> pending=new AtomicReference<>();
        @Override public Object intercept(Invocation invocation) throws Throwable {
            Object result=invocation.proceed();
            if(((MappedStatement)invocation.getArgs()[0]).getId().equals("com.cyys.admin.user.mapper.MemberMapper.export")) {
                var gate=pending.getAndSet(null);
                if(gate!=null) {
                    gate.loaded.countDown();
                    if(!gate.resume.await(15,TimeUnit.SECONDS)) throw new IllegalStateException("Export test barrier timed out");
                }
            }
            return result;
        }
    }

    private HttpResponse<byte[]> export(String token,String scope,String body) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/admin/users/export"))
                .timeout(Duration.ofSeconds(30)).header("Authorization","Bearer "+token)
                .header("X-Cyys-Scope-Id",scope).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    private List<List<String>> rows(HttpResponse<byte[]> response) throws Exception {
        assertThat(response.statusCode()).as(new String(response.body(),java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(200);
        var result=new ArrayList<List<String>>();
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(response.body()))) {
            for(var row:book.getSheetAt(0)) {
                var cells=new ArrayList<String>();
                for(var cell:row) {
                    assertThat(cell.getCellType()).isEqualTo(CellType.STRING);
                    assertThat(cell.getHyperlink()).isNull();
                    cells.add(cell.getStringCellValue());
                }
                assertThat(cells).hasSize(5); result.add(cells);
            }
        }
        return result;
    }
}
