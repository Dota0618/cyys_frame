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
    @org.springframework.beans.factory.annotation.Autowired com.cyys.common.storage.StorageProperties fileStorage;
    private Path fileRoot;

    @AfterEach void cleanupBrowserRoles() throws Exception {
        jdbc.update("delete from param_region where created_by='u-platform'");
        jdbc.update("delete from sys_file_binding where scope_id='GLOBAL' and resource='RELEASE' and record_id in (select id from sys_version where created_by in ('u-platform','g4-release-editor'))");
        jdbc.update("delete from sys_version where created_by in ('u-platform','g4-release-editor')");
        jdbc.update("delete from sys_notice where created_by='u-platform' and title like 'g4-browser-notice-%'");
        jdbc.update("delete from param_basic_type where basic_id='98311'");
        jdbc.update("delete from param_basic where id='98311'");
        jdbc.update("delete from param_basic_type where basic_id='98411'");
        jdbc.update("delete from param_basic where id='98411'");
        jdbc.update("delete from sys_file_binding where scope_id in ('scope-a','scope-b')");
        jdbc.update("delete from sys_org_notice_ref where scope_id in ('scope-a','scope-b')");
        jdbc.update("delete from sys_org_notice where scope_id in ('scope-a','scope-b')");
        if (fileRoot != null) {
            for (String key : jdbc.queryForList("select object_key from sys_file_part where scope_id in ('scope-a','scope-b') "
                    + "union all select object_key from sys_file_entry where scope_id in ('scope-a','scope-b')", String.class)) {
                assertThat(key).matches("[a-f0-9]{32}");
                java.nio.file.Files.deleteIfExists(fileRoot.resolve(key));
            }
            jdbc.update("delete from sys_file_part where scope_id in ('scope-a','scope-b')");
            jdbc.update("delete from sys_file_entry where scope_id in ('scope-a','scope-b')");
            jdbc.update("delete from sys_file_dir where scope_id in ('scope-a','scope-b')");
            java.nio.file.Files.delete(fileRoot);
        }
        jdbc.update("delete from param_basic_type where basic_id in "
                + "(select id from param_basic where system_name like 'g4-browser-global-%')");
        jdbc.update("delete from param_basic where system_name like 'g4-browser-global-%'");
        jdbc.update("delete from sys_configure where name like 'g4-browser-global-%'");
        jdbc.update("delete from param_basic_type where basic_id=98111");
        jdbc.update("delete from param_basic where id=98111");
        for (String id : jdbc.queryForList("select id from sys_role where code in ('G4_BROWSER_ROLE','G4_BROWSER_PLATFORM_ROLE','G4_BROWSER_GLOBAL_ROLE')", String.class)) {
            jdbc.update("delete from sys_user_role where role_id=?", id);
            jdbc.update("delete from sys_role_menu where role_id=?", id);
            jdbc.update("delete from sys_role_org where role_id=?", id);
            jdbc.update("delete from sys_role where id=?", id);
        }
        jdbc.update("delete from sys_role_menu where menu_id in (select id from sys_menu where code='g4-browser-menu')");
        jdbc.update("delete from sys_menu where code='g4-browser-menu'");
        jdbc.update("delete from sys_data_audit where scope_id='GLOBAL' and actor_id='u-platform'");
        jdbc.update("delete from sys_data_audit where scope_id='GLOBAL' and (actor_id='g4-release-editor' or actor_id in (select id from sys_user where login_name like 'g4-%'))");
    }

    @Test void actualBrowserFlows() throws Exception {
        fileRoot = java.nio.file.Files.createTempDirectory(Path.of("target").toAbsolutePath(), "browser-files-");
        var local = new com.cyys.common.storage.StorageProperties.Profile();
        local.setDriver("local"); local.setRoot(fileRoot.toString());
        fileStorage.setActive("browser"); fileStorage.setProfiles(java.util.Map.of("browser", local));
        fileStorage.setMaxBytes(4096); fileStorage.setPartBytes(1024); fileStorage.setMaxResumableBytes(8192);
        for (String permission : List.of("file:read", "file:upload", "file:update", "file:delete", "file:content:read")) allow("role-a",permission,false);
        for (String action : List.of("move","directory:create","directory:update","directory:delete","recycle:read","restore","purge")) allow("role-a","file:"+action,false);
        for (String permission : List.of("file:read", "file:content:read")) allow("role-b",permission,false);
        for (String action : List.of("read","create","update","delete","receipts:read")) allow("role-a","org-notice:"+action,false);
        allow("role-b","org-notice:read",false);
        jdbc.update("insert into param_basic(id,system_name,name,created_by,updated_by) values('98111','NOTICE_TYPE','通知类型','u-alice','u-alice')");
        jdbc.update("insert into param_basic_type(id,basic_id,param_key,param_value,created_by,updated_by) values('98112','98111','normal','普通通知','u-alice','u-alice')");
        for (String permission : List.of("user:read","user:create","user:update","user:export","role:grant",
                "role:read","role:create","role:update","role:delete")) allow("role-a",permission,false);
        allow("role-b","user:read",false);
        allow("role-a", "cache:user:refresh", false);
        for (String permission : List.of("audit:read", "login-audit:read")) {
            allow("role-a", permission, false);
            allow("role-b", permission, false);
        }
        membership("u-alice","scope-b",false);
        grant("u-alice","role-b","scope-b");
        jdbc.update("update sys_user_scope set display_name=case when scope_id='scope-a' then '单位 A 姓名' else '单位 B 姓名' end");
        platformUser();
        for (String permission : List.of("user:read","user:create")) allow("role-platform",permission,false);
        allow("role-platform","platform:data:read-all",true);
        allow("role-platform","platform:account:reset-password",true);
        for (String permission : List.of("menu:read","menu:create","menu:update","menu:delete",
                "platform:role:read","platform:role:create","platform:role:update","platform:role:delete",
                "platform:role:grant")) allow("role-platform",permission,true);
        for (String resource : List.of("configuration", "dictionary", "system-notice", "release", "region")) {
            for (String operation : List.of("read", "create", "update", "delete")) {
                allow("role-platform", resource + ":" + operation, true);
            }
        }
        allow("role-platform","region:export",true);
        user("g4-release-editor","g4-release-editor");
        role("g1-release","GLOBAL","RELEASE_EDITOR");
        grant("g4-release-editor","g1-release","GLOBAL");
        for(String permission:List.of("platform:scope:access","platform:data:operate","release:read","release:create","release:update","release:delete","file:upload","file:read","file:content:read"))
            allow("g1-release",permission,true);
        jdbc.update("insert into param_basic(id,system_name,name) values('98411','VERSION_TYPE','版本类型')");
        jdbc.update("insert into param_basic_type(id,basic_id,param_key,param_value) values('98412','98411','normal','常规更新')");

        registerNavigation();
        Path frontend=Path.of("..","..","admin-ui").toAbsolutePath().normalize();
        String logPrefix = System.getProperty("cyys.browser.log-prefix", "phase4");
        if (!logPrefix.matches("[a-zA-Z0-9_-]+")) throw new IllegalArgumentException("Invalid browser log prefix");
        Path log=Path.of("..","logs",logPrefix + "_browser_out.log").toAbsolutePath().normalize();
        String node=Path.of(System.getenv("NODE_HOME"),"node.exe").toString();
        var builder=new ProcessBuilder(node,"node_modules/@playwright/test/cli.js","test")
                .directory(frontend.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        String spec = System.getProperty("cyys.browser.spec");
        if (spec != null) {
            for (String item : spec.split(",", -1)) {
                if (!item.matches("[a-z-]+\\.spec\\.ts")) throw new IllegalArgumentException("Invalid browser spec");
                builder.command().add(item);
            }
        }
        builder.environment().put("CYYS_API_TARGET","http://127.0.0.1:"+port);
        // 前端测试进程只需要 HTTP 地址，不继承数据库和 Redis 凭据。
        builder.environment().keySet().removeIf(name -> name.startsWith("CYYS_TEST_"));
        Process process=builder.start();
        try {
            assertThat(process.waitFor(600,TimeUnit.SECONDS)).as("browser timeout; see %s",log).isTrue();
            assertThat(process.exitValue()).as("browser results: %s",log).isZero();
        } finally {
            process.descendants().forEach(child -> child.destroyForcibly());
            if(process.isAlive()) process.destroyForcibly();
        }
    }

    /** Browser fixtures explicitly grant page nodes, as real role assignments do. */
    private void registerNavigation() {
        String[][] pages = {
                {"users","用户管理","user:read"}, {"departments","部门管理","department:read"},
                {"roles","角色管理","role:read"}, {"platform-menus","平台菜单","menu:read"},
                {"platform-roles","平台角色","platform:role:read"}, {"configurations","通用配置","configuration:read"},
                {"dictionaries","数据字典","dictionary:read"}, {"audits","操作审计","audit:read"},
                {"login-audits","登录记录","login-audit:read"}, {"caches","缓存管理","cache:user:refresh"},
                {"files","文件管理","file:read"}, {"file-recycle","文件回收站","file:recycle:read"},
                {"org-notices","单位通知","org-notice:read"}, {"system-notices","系统通知","system-notice:read"},
                {"releases","版本更新","release:read"}, {"regions","地区参数","region:read"}
        };
        for (String[] page : pages) {
            String id="g1-nav-"+page[0];
            jdbc.update("insert into sys_menu(id,code,name,type,path) values(?,?,?,2,?)",id,"nav:"+page[0],page[1],"/system/"+page[0]);
            for (String role : jdbc.queryForList("select distinct rm.role_id from sys_role_menu rm join sys_menu m on m.id=rm.menu_id where m.code=? and rm.deleted=0",String.class,page[2]))
                jdbc.update("insert into sys_role_menu(id,role_id,menu_id) values(?,?,?)",java.util.UUID.randomUUID().toString().replace("-",""),role,id);
        }
    }
}
