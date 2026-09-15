package com.cyys.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"cyys.data-isolation.mode=multi","cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class MemberIntegrationTest extends IsolationTestSupport {
    static final String USERS="/api/admin/users";
    @BeforeEach void memberPermissions() {
        for(String permission:List.of("user:read","user:create","user:update","role:grant","user:display:read")) allow("role-a",permission,false);
        allow("role-b","user:read",false);
        jdbc.update("update sys_user_scope set display_name='单位 A 姓名' where user_id='u-alice'");
        jdbc.update("update sys_user_scope set display_name='单位 B 姓名' where user_id='u-bob'");
    }
    Reply users(String method,String suffix,String token,String scope,Object body) throws Exception {
        return request(method,USERS+suffix,token,scope,body==null?null:JSON.writeValueAsString(body));
    }
    @Test void listsDetailsPagingAndMemberRangesShareBoundary() throws Exception {
        String token=token("alice");
        var page=users("GET","?size=1",token,"scope-a",null);
        assertThat(page.status()).as(page.raw()).isEqualTo(200);
        assertThat(page.body().at("/data/total").asInt()).isEqualTo(1);
        assertThat(page.raw()).contains("单位 A 姓名").doesNotContain("单位 B 姓名","password","idCard");
        assertThat(users("GET","/u-bob-scope-b",token,"scope-a",null).status()).isEqualTo(404);
        assertThat(users("GET","?size=201",token,"scope-a",null).status()).isEqualTo(400);
        assertThat(users("GET","",token,null,null).status()).isEqualTo(403);
        membership("u-bob","scope-a",false);
        jdbc.update("update sys_user_scope set org_id='g1-a-child' where user_id='u-bob' and scope_id='scope-a'");
        jdbc.update("update sys_role set data_range=4 where id='role-a'");
        assertThat(users("GET","",token,"scope-a",null).body().at("/data/total").asInt()).isEqualTo(2);
        jdbc.update("update sys_role set data_range=5 where id='role-a'");
        assertThat(users("GET","",token,"scope-a",null).body().at("/data/total").asInt()).isEqualTo(1);
    }
    @Test void createUsesCurrentUnitRejectsPrivilegedFieldsAndRollsBackGlobalAccount() throws Exception {
        String token=token("alice");
        var body=new java.util.LinkedHashMap<String,Object>(Map.of("loginName","g4-created","password",PASSWORD,"displayName","新成员","orgId","g1-b-root"));
        assertThat(users("POST","",token,"scope-a",body).status()).isEqualTo(403);
        assertThat(count("select count(*) from sys_user where login_name='g4-created'")).isZero();
        body.put("orgId","g1-a-child"); body.put("scopeId","scope-b");
        assertThat(users("POST","",token,"scope-a",body).status()).isEqualTo(400);
        body.remove("scopeId");
        var result=users("POST","",token,"scope-a",body);
        assertThat(result.status()).as(result.raw()).isEqualTo(200);
        assertThat(result.body().at("/data/scopeId").asText()).isEqualTo("scope-a");
        assertThat(users("POST","",token,"scope-a",body).status()).isEqualTo(409);
        assertThat(login("g4-created",PASSWORD,null).status()).isEqualTo(200);
    }
    @Test void editingLocalNameAndStatusDoesNotChangeAnotherUnitOrGlobalAccount() throws Exception {
        membership("u-alice","scope-b",false); grant("u-alice","role-b","scope-b");
        jdbc.update("update sys_user_scope set display_name='另一单位姓名' where user_id='u-alice' and scope_id='scope-b'");
        String token=token("alice");
        ok("GET","/user-names?ids=u-alice",token,"scope-a",null);
        var edit=users("PUT","/u-alice-scope-a",token,"scope-a",Map.of("displayName","本单位新姓名","orgId","g1-a-child","status",1,"version",0));
        assertThat(edit.status()).as(edit.raw()).isEqualTo(200);
        assertThat(jdbc.queryForObject("select real_name from sys_user where id='u-alice'",String.class)).isEqualTo("alice");
        assertThat(jdbc.queryForObject("select display_name from sys_user_scope where user_id='u-alice' and scope_id='scope-b'",String.class)).isEqualTo("另一单位姓名");
        assertThat(ok("GET","/user-names?ids=u-alice",token,"scope-a",null).raw()).contains("本单位新姓名");
        assertThat(users("PUT","/u-alice-scope-a",token,"scope-a",Map.of("displayName","stale","status",1,"version",0)).status()).isEqualTo(409);
        var clear=users("PUT","/u-alice-scope-a",token,"scope-a",Map.of("displayName","本单位新姓名","status",1,"version",1));
        assertThat(clear.status()).as(clear.raw()).isEqualTo(200);
        assertThat(jdbc.queryForObject("select org_id from sys_user_scope where user_id='u-alice' and scope_id='scope-a'",String.class)).isNull();
    }
    @Test void directCallsCannotUseHiddenActionsAndGrantsAreScoped() throws Exception {
        String bob=token("bob");
        assertThat(users("POST","",bob,"scope-b",Map.of("loginName","g4-hidden","password",PASSWORD,"displayName","hidden")).status()).isEqualTo(403);
        String alice=token("alice");
        membership("u-bob","scope-a",false);
        assertThat(users("PUT","/u-bob-scope-a/roles",alice,"scope-a",List.of("role-b")).status()).isEqualTo(403);
        var granted=users("PUT","/u-bob-scope-a/roles",alice,"scope-a",List.of("role-a"));
        assertThat(granted.status()).as(granted.raw()).isEqualTo(200);
        assertThat(count("select count(*) from sys_user_role where user_id='u-bob' and scope_id='scope-b'")).isEqualTo(1);
        assertThat(users("GET","",bob,"scope-a",null).status()).isEqualTo(200);
        assertThat(users("PUT","/u-bob-scope-a/roles",alice,"scope-a",List.of()).status()).isEqualTo(200);
        assertThat(users("GET","",bob,"scope-a",null).status()).isEqualTo(403);
    }
    @Test void platformAllIsReadOnlyAndExplicitTargetKeepsRealActor() throws Exception {
        platformUser();
        for(String permission:List.of("user:read","user:create")) allow("role-platform",permission,false);
        allow("role-platform","platform:data:read-all",true);
        String token=token("platform");
        var all=users("GET","/all",token,null,null);
        assertThat(all.status()).as(all.raw()).isEqualTo(200);
        assertThat(all.body().at("/data/total").asInt()).isEqualTo(2);
        var body=Map.of("loginName","g4-platform","password",PASSWORD,"displayName","平台建立成员");
        assertThat(users("POST","",token,null,body).status()).isEqualTo(403);
        assertThat(users("POST","",token,"scope-b",body).status()).isEqualTo(403);
        allow("role-platform","platform:data:operate",true);
        var created=users("POST","",token,"scope-b",body);
        assertThat(created.status()).as(created.raw()).isEqualTo(200);
        assertThat(count("select count(*) from sys_data_audit where resource='user' and scope_id='scope-b' and actor_id='u-platform'")).isEqualTo(1);
    }
}
