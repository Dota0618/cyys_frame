package com.cyys.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

/** 显式运行：真实 Spring / MySQL / Redis + 已构建前端 + 本机 Chrome。 */
@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"cyys.data-isolation.mode=multi","cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class BrowserAcceptance extends IsolationTestSupport {
    @AfterEach void cleanupBrowserRoles() {
        for (String id : jdbc.queryForList("select id from sys_role where code='G4_BROWSER_ROLE'", String.class)) {
            jdbc.update("delete from sys_user_role where role_id=?", id);
            jdbc.update("delete from sys_role_menu where role_id=?", id);
            jdbc.update("delete from sys_role_org where role_id=?", id);
            jdbc.update("delete from sys_role where id=?", id);
        }
    }

    @Test void actualBrowserFlows() throws Exception {
        for (String permission : List.of("user:read","user:create","user:update","role:grant",
                "role:read","role:create","role:update","role:delete")) allow("role-a",permission,false);
        allow("role-b","user:read",false);
        membership("u-alice","scope-b",false);
        grant("u-alice","role-b","scope-b");
        jdbc.update("update sys_user_scope set display_name=case when scope_id='scope-a' then '单位 A 姓名' else '单位 B 姓名' end");
        platformUser();
        for (String permission : List.of("user:read","user:create")) allow("role-platform",permission,false);
        allow("role-platform","platform:data:read-all",true);

        Path frontend=Path.of("..","..","admin-ui").toAbsolutePath().normalize();
        Path log=Path.of("..","logs","phase4_browser_out.log").toAbsolutePath().normalize();
        String node=Path.of(System.getenv("NODE_HOME"),"node.exe").toString();
        var builder=new ProcessBuilder(node,"node_modules/@playwright/test/cli.js","test")
                .directory(frontend.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("CYYS_API_TARGET","http://127.0.0.1:"+port);
        // 前端测试进程只需要 HTTP 地址，不继承数据库和 Redis 凭据。
        builder.environment().keySet().removeIf(name -> name.startsWith("CYYS_TEST_"));
        Process process=builder.start();
        try {
            assertThat(process.waitFor(240,TimeUnit.SECONDS)).as("browser timeout; see %s",log).isTrue();
            assertThat(process.exitValue()).as("browser results: %s",log).isZero();
        } finally {
            process.descendants().forEach(child -> child.destroyForcibly());
            if(process.isAlive()) process.destroyForcibly();
        }
    }
}
