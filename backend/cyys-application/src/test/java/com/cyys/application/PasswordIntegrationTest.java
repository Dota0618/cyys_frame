package com.cyys.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"cyys.data-isolation.mode=multi","cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordIntegrationTest extends HttpTestSupport {
    private static final String NEW_PASSWORD="changed-password-2026";

    @AfterEach void clearAccountAudit() {
        jdbc.update("delete from sys_data_audit where scope_id='GLOBAL' and resource='account' "
                + "and user_id in ('u-alice','u-platform') and record_id in ('u-alice','u-bob','u-platform')");
    }

    @Test void ordinaryAccountChangesOwnPasswordWithoutPlatformGrantAndInvalidatesOldSession() throws Exception {
        String oldToken=token("alice");
        var changed=change(oldToken,PASSWORD,NEW_PASSWORD);
        assertThat(changed.status()).as(changed.raw()).isEqualTo(200);
        assertThat(request("GET","/api/auth/me",oldToken,null,null).status()).isEqualTo(401);
        assertThat(login("alice",PASSWORD,null).status()).isEqualTo(401);
        assertThat(login("alice",NEW_PASSWORD,null).status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("select count(*) from sys_data_audit where scope_id='GLOBAL' and resource='account' "
                + "and user_id='u-alice' and record_id='u-alice' and operation='CHANGE_PASSWORD' and authorized_view='ACCOUNT_SELF'",Integer.class))
                .isEqualTo(1);
    }

    @Test void wrongCurrentPasswordCommitsFailureCountAndLocksAtConfiguredLimit() throws Exception {
        String token=token("alice");
        for(int attempt=1;attempt<=5;attempt++) {
            assertThat(change(token,"wrong-password",NEW_PASSWORD).status()).isEqualTo(attempt==5?423:400);
            assertThat(jdbc.queryForObject("select pwd_error_count from sys_user where id='u-alice'",Integer.class)).isEqualTo(attempt);
        }
        assertThat(change(token,PASSWORD,NEW_PASSWORD).status()).isEqualTo(423);
        assertThat(jdbc.queryForObject("select count(*) from sys_data_audit where scope_id='GLOBAL' and resource='account' and user_id='u-alice'",Integer.class))
                .isZero();
    }

    @Test void administratorResetsWithoutOwnPasswordAndRevokedGrantIsRejected() throws Exception {
        platformUser();
        permission("role-platform","g1-reset","platform:account:reset-password",true);
        String admin=token("platform"), old=token("alice");
        var reset=reset(admin,"u-alice");
        assertThat(reset.status()).as(reset.raw()).isEqualTo(200);
        assertThat(request("GET","/api/auth/me",old,null,null).status()).isEqualTo(401);
        assertThat(login("alice",NEW_PASSWORD,null).status()).isEqualTo(200);
        assertThat(request("GET","/api/auth/me",admin,null,null).status()).isEqualTo(200);
        jdbc.update("update sys_user set status=0,pwd_error_count=5,pwd_lock_time=DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 15 MINUTE) where id='u-bob'");
        assertThat(reset(admin,"u-bob").status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("select status from sys_user where id='u-bob'",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select pwd_error_count from sys_user where id='u-bob'",Integer.class)).isZero();
        jdbc.update("update sys_role_menu set deleted=1 where role_id='role-platform' and menu_id='g1-reset'");
        assertThat(reset(admin,"u-alice").status()).isEqualTo(403);
    }

    @Test void unitRoleCannotGrantAccountResetAndUnknownResetFieldsAreRejected() throws Exception {
        permission("role-a","g1-reset","platform:account:reset-password",false);
        assertThat(reset(token("alice"),"u-bob").status()).isEqualTo(403);
        platformUser();
        jdbc.update("insert into sys_role_menu(id,role_id,menu_id) values('g1-reset-platform','role-platform','g1-reset')");
        var reply=request("POST","/api/admin/platform/accounts/u-bob/password",token("platform"),null,
                JSON.writeValueAsString(Map.of("newPassword",NEW_PASSWORD,"adminPassword",PASSWORD)));
        assertThat(reply.status()).as(reply.raw()).isEqualTo(400);
    }

    private Reply change(String token,String current,String replacement) throws Exception {
        return request("POST","/api/auth/password",token,null,
                JSON.writeValueAsString(Map.of("currentPassword",current,"newPassword",replacement)));
    }

    private Reply reset(String token,String target) throws Exception {
        return request("POST","/api/admin/platform/accounts/"+target+"/password",token,null,
                JSON.writeValueAsString(Map.of("newPassword",NEW_PASSWORD)));
    }
}
