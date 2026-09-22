package com.cyys.application;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class RecycleIntegrationTest extends HttpTestSupport {
    private static final String ROOT = "/api/admin/recycle";

    @BeforeEach void setupRecycle() {
        jdbc.update("update sys_user_scope set data_range=1 where user_id in ('u-alice','u-bob')");
        for (String role : List.of("role-a", "role-b")) {
            for (String permission : List.of("recycle:read", "recycle:restore", "recycle:purge",
                    "org-notice:read", "org-notice:create", "org-notice:delete")) allow(role, permission);
        }
        for (String resource : List.of("system-notice", "release")) {
            for (String action : List.of("read", "create", "delete")) allow("role-a", resource + ":" + action);
        }
        allow("role-a", "file:delete");
        jdbc.update("insert into sys_org(id,scope_id,name,code,created_by) values "
                + "('g1-recycle-dept-a','scope-a','部门 A','RECYCLE_A','u-alice'),"
                + "('g1-recycle-dept-b','scope-b','部门 B','RECYCLE_B','u-bob')");
    }

    @AfterEach void cleanupRecycle() {
        jdbc.update("delete from sys_file_binding where record_id like 'g1-recycle-%' or file_id='g1-recycle-file'");
        jdbc.update("delete from sys_org_notice_ref where notice_id like 'g1-recycle-%'");
        jdbc.update("delete from sys_org_notice where created_by in ('u-alice','u-bob')");
        jdbc.update("delete from sys_notice where id like 'g1-recycle-%'");
        jdbc.update("delete from sys_version where id like 'g1-recycle-%'");
        jdbc.update("delete from sys_file_entry where id='g1-recycle-file'");
        jdbc.update("delete from sys_data_audit where actor_id in ('u-alice','u-bob')");
        jdbc.update("update sys_menu set status=1 where created_by='migration-v27'");
    }

    @Test void metadataAndEveryEndpointEnforceResourceAndActionPermissions() throws Exception {
        String alice = token("alice"), bob = token("bob");
        assertThat(request("GET", ROOT + "/resources", null, null, null).status()).isEqualTo(401);
        var resources = request("GET", ROOT + "/resources", bob, null, null);
        assertThat(resources.status()).as(resources.raw()).isEqualTo(200);
        assertThat(resources.body().at("/data").size()).isEqualTo(1);
        assertThat(resources.raw()).contains("org-notice").doesNotContain("system-notice", "sys_org_notice", "tableName");
        assertThat(request("GET", ROOT + "/system-notice", bob, null, null).status()).isEqualTo(403);
        assertThat(change("system-notice", "restore", bob, null, ref("missing", 0)).status()).isEqualTo(403);
        assertThat(request("GET", ROOT + "/sys-user", alice, null, null).status()).isEqualTo(404);
        assertThat(request("GET", ROOT + "/sys_notice", alice, null, null).status()).isEqualTo(400);
        seedNotice("g1-recycle-a", "scope-a", "u-alice", 1);
        revoke("role-a", "org-notice:create");
        assertThat(change("org-notice", "restore", alice, "scope-a", ref("g1-recycle-a", 0)).status()).isEqualTo(403);
        revoke("role-a", "recycle:purge");
        assertThat(change("org-notice", "purge", alice, "scope-a", ref("g1-recycle-a", 0)).status()).isEqualTo(403);
        revoke("role-a", "recycle:read");
        assertThat(request("GET", ROOT + "/resources", alice, "scope-a", null).status()).isEqualTo(403);
        assertThat(change("org-notice", "restore", alice, "scope-a", ref("g1-recycle-a", 0)).status()).isEqualTo(403);
    }

    @Test void listAndMutationsRespectInstitutionDepartmentAndOwnerRanges() throws Exception {
        seedNotice("g1-recycle-a", "scope-a", "u-alice", 1);
        seedNotice("g1-recycle-b", "scope-b", "u-bob", 1);
        seedNotice("g1-recycle-other", "scope-a", "u-bob", 1);
        seedNotice("g1-recycle-live", "scope-a", "u-alice", 0);
        String alice = token("alice");
        var page = request("GET", ROOT + "/org-notice?size=1", alice, "scope-a", null);
        assertThat(page.status()).as(page.raw()).isEqualTo(200);
        assertThat(page.body().at("/data/total").asInt()).isEqualTo(2);
        assertThat(page.body().at("/data/records").size()).isEqualTo(1);
        assertThat(page.raw()).doesNotContain("secret body", "g1-recycle-live", "g1-recycle-b");
        assertThat(request("GET", ROOT + "/org-notice?search=absent", alice, "scope-a", null).body().at("/data/total").asInt()).isZero();
        assertThat(request("GET", ROOT + "/org-notice", alice, null, null).status()).isEqualTo(403);
        assertThat(request("GET", ROOT + "/org-notice", alice, "scope-b", null).status()).isEqualTo(403);
        for (String action : List.of("restore", "purge")) {
            assertThat(change("org-notice", action, alice, "scope-a", ref("g1-recycle-b", 0)).status()).isEqualTo(409);
            assertThat(change("org-notice", action, alice, "scope-a", ref("g1-recycle-live", 0)).status()).isEqualTo(409);
        }
        jdbc.update("update sys_user_scope set data_range=5 where user_id='u-alice'");
        assertThat(request("GET", ROOT + "/org-notice", alice, "scope-a", null).body().at("/data/total").asInt()).isEqualTo(1);
        assertThat(change("org-notice", "purge", alice, "scope-a", ref("g1-recycle-other", 0)).status()).isEqualTo(409);
        jdbc.update("update sys_user_scope set data_range=3,org_id='g1-recycle-dept-a' where user_id='u-alice'");
        assertThat(request("GET", ROOT + "/org-notice", alice, "scope-a", null).body().at("/data/total").asInt()).isEqualTo(2);
    }

    @Test void softDeleteRetainsAttachmentsAndRestoreKeepsOriginalOwnershipAndReceipts() throws Exception {
        seedNotice("g1-recycle-a", "scope-a", "u-alice", 0);
        seedAttachment("ORG_NOTICE", "scope-a", "g1-recycle-a");
        jdbc.update("insert into sys_org_notice_ref(scope_id,notice_id,user_id) values('scope-a','g1-recycle-a','u-alice')");
        String alice = token("alice");
        assertThat(request("DELETE", "/api/admin/org-notices/g1-recycle-a?version=0", alice, "scope-a", null).status()).isEqualTo(200);
        assertThat(count("select count(*) from sys_file_binding where record_id='g1-recycle-a'")).isEqualTo(1);
        assertThat(request("DELETE", "/api/admin/files/g1-recycle-file?version=0", alice, "scope-a", null).status()).isEqualTo(409);
        var restored = change("org-notice", "restore", alice, "scope-a", ref("g1-recycle-a", 1));
        assertThat(restored.status()).as(restored.raw()).isEqualTo(200);
        assertThat(jdbc.queryForMap("select scope_id,org_id,created_by,version,deleted from sys_org_notice where id='g1-recycle-a'"))
                .containsEntry("scope_id", "scope-a").containsEntry("org_id", "g1-recycle-dept-a")
                .containsEntry("created_by", "u-alice").containsEntry("version", 2);
        assertThat(request("GET", "/api/admin/org-notices/g1-recycle-a", alice, "scope-a", null).raw()).contains("g1-recycle-file", "secret body");
        assertThat(count("select count(*) from sys_org_notice_ref where notice_id='g1-recycle-a'")).isEqualTo(1);
        assertThat(audits("RESTORE")).isEqualTo(1);
        assertThat(change("org-notice", "restore", alice, "scope-a", ref("g1-recycle-a", 1)).status()).isEqualTo(409);
        assertThat(audits("RESTORE")).isEqualTo(1);
    }

    @Test void invalidDepartmentOrAttachmentBlocksRestoreWithoutChangingHistory() throws Exception {
        seedNotice("g1-recycle-a", "scope-a", "u-alice", 1);
        String alice = token("alice");
        jdbc.update("update sys_org set status=0 where id='g1-recycle-dept-a'");
        assertThat(change("org-notice", "restore", alice, "scope-a", ref("g1-recycle-a", 0)).status()).isEqualTo(409);
        jdbc.update("update sys_org set status=1,deleted=1 where id='g1-recycle-dept-a'");
        assertThat(change("org-notice", "restore", alice, "scope-a", ref("g1-recycle-a", 0)).status()).isEqualTo(409);
        jdbc.update("update sys_org set deleted=0 where id='g1-recycle-dept-a'");
        seedAttachment("ORG_NOTICE", "scope-a", "g1-recycle-a");
        jdbc.update("update sys_file_entry set deleted=1 where id='g1-recycle-file'");
        assertThat(change("org-notice", "restore", alice, "scope-a", ref("g1-recycle-a", 0)).status()).isEqualTo(409);
        assertThat(count("select count(*) from sys_org_notice where id='g1-recycle-a' and deleted=1 and version=0")).isEqualTo(1);
        assertThat(audits("RESTORE")).isZero();
    }

    @Test void batchRestoreRollsBackEarlierWritesAndAuditOnAnyVersionConflict() throws Exception {
        seedNotice("g1-recycle-a", "scope-a", "u-alice", 1);
        seedNotice("g1-recycle-z", "scope-a", "u-alice", 1);
        String alice = token("alice");
        assertThat(change("org-notice", "restore", alice, "scope-a", ref("g1-recycle-a", 0), ref("g1-recycle-z", 7)).status()).isEqualTo(409);
        assertThat(count("select count(*) from sys_org_notice where deleted=1 and version=0")).isEqualTo(2);
        assertThat(audits("RESTORE")).isZero();
        assertThat(change("org-notice", "restore", alice, "scope-a", ref("g1-recycle-z", 0), ref("g1-recycle-a", 0)).status()).isEqualTo(200);
        assertThat(audits("RESTORE")).isEqualTo(2);
    }

    @Test void batchPurgeIsAtomicAndOnlyClearsTheParentsRelations() throws Exception {
        seedNotice("g1-recycle-a", "scope-a", "u-alice", 1);
        seedNotice("g1-recycle-z", "scope-a", "u-alice", 1);
        seedAttachment("ORG_NOTICE", "scope-a", "g1-recycle-a");
        jdbc.update("insert into sys_org_notice_ref(scope_id,notice_id,user_id) values('scope-a','g1-recycle-a','u-alice')");
        String alice = token("alice");
        assertThat(change("org-notice", "purge", alice, "scope-a", ref("g1-recycle-a", 0), ref("g1-recycle-z", 3)).status()).isEqualTo(409);
        assertThat(count("select count(*) from sys_org_notice")).isEqualTo(2);
        assertThat(count("select count(*) from sys_file_binding where record_id='g1-recycle-a'")).isEqualTo(1);
        assertThat(count("select count(*) from sys_org_notice_ref where notice_id='g1-recycle-a'")).isEqualTo(1);
        assertThat(audits("PURGE")).isZero();
        assertThat(change("org-notice", "purge", alice, "scope-a", ref("g1-recycle-a", 0), ref("g1-recycle-z", 0)).status()).isEqualTo(200);
        assertThat(count("select count(*) from sys_org_notice")).isZero();
        assertThat(count("select count(*) from sys_org_notice_ref where notice_id='g1-recycle-a'")).isZero();
        assertThat(count("select count(*) from sys_file_binding where record_id='g1-recycle-a'")).isZero();
        assertThat(count("select count(*) from sys_file_entry where id='g1-recycle-file' and deleted=0")).isEqualTo(1);
        assertThat(audits("PURGE")).isEqualTo(2);
    }

    @Test void sharedResourcesWorkWithoutSelectedInstitutionAndKeepContentAndAttachments() throws Exception {
        jdbc.update("insert into sys_notice(id,title,type,type_label,content,is_show,created_by,updated_by,deleted) "
                + "values('g1-recycle-system','共享通知','historical','历史类型','原正文',1,'u-alice','u-alice',1)");
        jdbc.update("insert into sys_version(id,release_number,date_update,type,type_label,content,created_by,updated_by) "
                + "values('g1-recycle-release','1.0','2026-09-22','old','历史版本','原版本内容','u-alice','u-alice')");
        seedAttachment("RELEASE", "GLOBAL", "g1-recycle-release");
        String alice = token("alice");
        assertThat(request("DELETE", "/api/admin/platform/releases/g1-recycle-release?version=0", alice, null, null).status()).isEqualTo(200);
        assertThat(count("select count(*) from sys_file_binding where record_id='g1-recycle-release'")).isEqualTo(1);
        for (String resource : List.of("system-notice", "release")) {
            var page = request("GET", ROOT + "/" + resource, alice, null, null);
            assertThat(page.status()).as(page.raw()).isEqualTo(200);
            assertThat(page.body().at("/data/total").asInt()).isEqualTo(1);
        }
        assertThat(change("system-notice", "restore", alice, null, ref("g1-recycle-system", 0)).status()).isEqualTo(200);
        assertThat(change("release", "restore", alice, null, ref("g1-recycle-release", 1)).status()).isEqualTo(200);
        assertThat(count("select count(*) from sys_notice where deleted=0 and is_show=1 and content='原正文'")).isEqualTo(1);
        assertThat(count("select count(*) from sys_file_binding where record_id='g1-recycle-release'")).isEqualTo(1);
        assertThat(count("select count(*) from sys_data_audit where operation='RESTORE' and scope_id='GLOBAL'")).isEqualTo(2);
        assertThat(request("DELETE", "/api/admin/platform/releases/g1-recycle-release?version=2", alice, null, null).status()).isEqualTo(200);
        assertThat(change("release", "purge", alice, null, ref("g1-recycle-release", 3)).status()).isEqualTo(200);
        assertThat(count("select count(*) from sys_file_binding where record_id='g1-recycle-release'")).isZero();
        assertThat(count("select count(*) from sys_file_entry where id='g1-recycle-file'")).isEqualTo(1);
        assertThat(request("DELETE", "/api/admin/platform/system-notices/g1-recycle-system?version=1", alice, null, null).status()).isEqualTo(200);
        assertThat(change("system-notice", "purge", alice, null, ref("g1-recycle-system", 2)).status()).isEqualTo(200);
        assertThat(count("select count(*) from sys_notice where id='g1-recycle-system'")).isZero();
    }

    @Test void invalidCommandsDoNotWriteOrAcceptTableAndOwnershipInjection() throws Exception {
        seedNotice("g1-recycle-a", "scope-a", "u-alice", 1);
        String alice = token("alice");
        for (String body : List.of("{}", "{\"records\":[]}", "{\"records\":[null]}",
                "{\"records\":[{\"id\":\"g1-recycle-a\",\"version\":-1}]}",
                "{\"records\":[{\"id\":\"g1-recycle-a\",\"version\":0,\"scopeId\":\"scope-b\"}]}",
                "{\"records\":[{\"id\":\"g1-recycle-a\"}]}",
                JSON.writeValueAsString(Map.of("records", Collections.nCopies(101, ref("g1-recycle-a", 0)))))) {
            assertThat(request("POST", ROOT + "/org-notice/restore", alice, "scope-a", body).status()).as(body).isEqualTo(400);
        }
        assertThat(change("org-notice", "purge", alice, "scope-a", ref("g1-recycle-a", 0), ref("g1-recycle-a", 0)).status()).isEqualTo(400);
        assertThat(request("GET", ROOT + "/org-notice?size=101", alice, "scope-a", null).status()).isEqualTo(400);
        assertThat(count("select count(*) from sys_org_notice where deleted=1 and version=0")).isEqualTo(1);
        assertThat(audits("RESTORE")).isZero();
    }

    @Test void concurrentRestoreAndPurgeAllowExactlyOneCommit() throws Exception {
        seedNotice("g1-recycle-a", "scope-a", "u-alice", 1);
        String alice = token("alice");
        try (var workers = Executors.newFixedThreadPool(2)) {
            var ready = new CountDownLatch(1);
            var restore = workers.submit(() -> { ready.await(); return change("org-notice", "restore", alice, "scope-a", ref("g1-recycle-a", 0)); });
            var purge = workers.submit(() -> { ready.await(); return change("org-notice", "purge", alice, "scope-a", ref("g1-recycle-a", 0)); });
            ready.countDown();
            assertThat(List.of(restore.get(20, TimeUnit.SECONDS).status(), purge.get(20, TimeUnit.SECONDS).status())).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(audits("RESTORE") + audits("PURGE")).isEqualTo(1);
    }

    private void allow(String role, String permission) {
        var ids = jdbc.queryForList("select id from sys_menu where code=?", String.class, permission);
        String id;
        if (ids.isEmpty()) {
            id = "g1-" + UUID.randomUUID().toString().replace("-", "").substring(0, 28);
            jdbc.update("insert into sys_menu(id,code,name,type) values(?,?,?,3)", id, permission, permission);
        } else id = ids.getFirst();
        jdbc.update("insert into sys_role_menu(id,role_id,menu_id) values(?,?,?)", UUID.randomUUID().toString().replace("-", ""), role, id);
    }

    private void revoke(String role, String permission) {
        jdbc.update("delete from sys_role_menu where role_id=? and menu_id=(select id from sys_menu where code=?)", role, permission);
    }

    private void seedNotice(String id, String scope, String owner, int deleted) {
        jdbc.update("insert into sys_org_notice(id,scope_id,org_id,title,content,created_by,updated_by,deleted,created_at,updated_at) values(?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                id, scope, scope.equals("scope-a") ? "g1-recycle-dept-a" : "g1-recycle-dept-b", id, "secret body", owner, owner, deleted);
    }

    private void seedAttachment(String resource, String scope, String record) {
        jdbc.update("insert into sys_file_entry(id,scope_id,org_id,name,storage_profile,object_key,state,byte_size,created_by,updated_by,created_at,updated_at) "
                + "values('g1-recycle-file','scope-a','g1-recycle-dept-a','附件','local','recycle-test-object','READY',10,'u-alice','u-alice',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
        jdbc.update("insert into sys_file_binding(scope_id,resource,record_id,file_id,file_scope_id,sort) values(?,?,?,'g1-recycle-file','scope-a',0)", scope, resource, record);
    }

    @SafeVarargs
    private Reply change(String resource, String action, String token, String scope, Map<String, Object>... records) throws Exception {
        return request("POST", ROOT + "/" + resource + "/" + action, token, scope, JSON.writeValueAsString(Map.of("records", List.of(records))));
    }
    private static Map<String, Object> ref(String id, int version) { return Map.of("id", id, "version", version); }
    private long count(String sql) { return jdbc.queryForObject(sql, Long.class); }
    private long audits(String operation) { return count("select count(*) from sys_data_audit where operation='" + operation + "'"); }
}
