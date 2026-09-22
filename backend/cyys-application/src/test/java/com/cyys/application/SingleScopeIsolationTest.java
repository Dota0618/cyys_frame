package com.cyys.application;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class SingleScopeIsolationTest extends IsolationTestSupport {
    @Test
    void memberPagesKeepSingleScopeCeiling() throws Exception {
        platformUser();
        allow("role-platform","user:read",false);
        allow("role-platform","platform:data:read-all",true);
        String platform=token("platform");
        var all=request("GET","/api/admin/users/all",platform,null,null);
        assertThat(all.status()).as(all.raw()).isEqualTo(200);
        assertThat(all.body().at("/data/total").asInt()).isEqualTo(1);
        assertThat(all.raw()).contains("u-alice").doesNotContain("u-bob","scope-b");
        assertThat(request("GET","/api/admin/users/u-bob-scope-b",platform,null,null).status()).isEqualTo(404);
        assertThat(request("GET","/api/admin/users",platform,"scope-b",null).status()).isEqualTo(403);
    }

    @Test
    void singleScopeCeilingAlsoAppliesToPlatformAcrossScopeReads() throws Exception {
        platformUser();
        allow("role-platform","department:read",false);
        allow("role-platform","platform:data:read-all",true);
        String platform=token("platform");
        var reply=ok("GET","/global",platform,null,null);
        assertThat(reply.body().path("data").size()).isEqualTo(3);
        assertThat(reply.raw()).doesNotContain("g1-b-root","scope-b");
        assertThat(call("GET","/departments/g1-b-root",platform,null,null).status()).isEqualTo(404);
        assertThat(call("GET","/global",platform,"scope-b",null).status()).isEqualTo(403);
        assertThat(count("select count(*) from sys_org where scope_id='scope-b'")).isEqualTo(1);
    }
}
