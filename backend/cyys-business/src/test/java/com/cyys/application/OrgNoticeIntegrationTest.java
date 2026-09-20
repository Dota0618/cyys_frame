package com.cyys.application;

import com.cyys.common.storage.StorageProperties;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"cyys.data-isolation.mode=multi","cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class OrgNoticeIntegrationTest extends IsolationTestSupport {
    private static final String BASE="/api/admin/org-notices";
    @Autowired StorageProperties storage;
    private Path root;

    @BeforeEach void noticeFixtures() throws Exception {
        root=Files.createTempDirectory(Path.of("target").toAbsolutePath(),"notice-files-");
        var profile=new StorageProperties.Profile(); profile.setDriver("local"); profile.setRoot(root.toString());
        storage.setActive("notice"); storage.setProfiles(Map.of("notice",profile));
        for(String role:List.of("role-a","role-b")) {
            for(String action:List.of("read","create","update","delete","receipts:read")) allow(role,"org-notice:"+action,false);
            for(String action:List.of("read","upload","content:read","delete","purge")) allow(role,"file:"+action,false);
        }
        jdbc.update("insert into param_basic(id,name,system_name,created_by,updated_by) values('98101','通知类型','NOTICE_TYPE','u-alice','u-alice')");
        jdbc.update("insert into param_basic_type(id,basic_id,param_key,param_value,created_by,updated_by) values('98102','98101','normal','一般通知','u-alice','u-alice')");
    }
    @AfterEach void cleanupNotices() throws Exception {
        jdbc.update("delete from sys_file_binding where scope_id in ('scope-a','scope-b')");
        jdbc.update("delete from sys_org_notice_ref where scope_id in ('scope-a','scope-b')");
        jdbc.update("delete from sys_org_notice where scope_id in ('scope-a','scope-b')");
        for(String key:jdbc.queryForList("select object_key from sys_file_entry where scope_id in ('scope-a','scope-b')",String.class)) {
            assertThat(key).matches("[a-f0-9]{32}"); Files.deleteIfExists(root.resolve(key));
        }
        jdbc.update("delete from sys_file_entry where scope_id in ('scope-a','scope-b')");
        jdbc.update("delete from param_basic_type where basic_id='98101'");
        jdbc.update("delete from param_basic where id='98101'");
        Files.delete(root);
    }

    @Test void noticeCrudIsScopedVersionedAndKeepsPlainText() throws Exception {
        String alice=token("alice"),bob=token("bob");
        var created=create(alice,"scope-a","g1-a-root",List.of());
        assertThat(created.status()).as(created.raw()).isEqualTo(200);
        String id=created.body().at("/data/notice/id").asText();
        assertThat(created.body().at("/data/content").asText()).isEqualTo("<script>plain text</script>");
        assertThat(request("GET",BASE+"/"+id,bob,"scope-b",null).status()).isEqualTo(404);
        assertThat(request("POST",BASE+"/"+id+"/read",bob,"scope-b",null).status()).isEqualTo(404);
        assertThat(request("GET",BASE+"?title=no-match",alice,"scope-a",null).body().at("/data/total").asInt()).isZero();
        assertThat(create(alice,"scope-a","g1-b-root",List.of()).status()).isEqualTo(409);
        assertThat(request("GET",BASE+"?size=101",alice,"scope-a",null).status()).isEqualTo(400);
        jdbc.update("update sys_org_notice set version=128 where id=?",id);
        assertThat(update(id,alice,128,List.of(),"normal").status()).isEqualTo(200);
        assertThat(update(id,alice,128,List.of(),"normal").status()).isEqualTo(409);
        jdbc.update("update sys_menu set status=0 where code='org-notice:update'");
        assertThat(update(id,alice,129,List.of(),"normal").status()).isEqualTo(403);
        assertThat(request("GET",BASE+"/"+id+"/edit",alice,"scope-a",null).status()).isEqualTo(403);
        assertThat(request("DELETE",BASE+"/"+id+"?version=129",alice,"scope-a",null).status()).isEqualTo(200);
        assertThat(request("GET",BASE+"/"+id,alice,"scope-a",null).status()).isEqualTo(404);
    }

    @Test void attachmentsUseParentAuthorizationAndUnlinkNeverDeletesTheObject() throws Exception {
        String alice=token("alice"),bob=token("bob"); String file=upload(alice,"scope-a","notice-attachment.txt");
        var created=create(alice,"scope-a","g1-a-root",List.of(file));
        assertThat(created.status()).as(created.raw()).isEqualTo(200);
        String id=created.body().at("/data/notice/id").asText();
        assertThat(request("DELETE","/api/admin/files/"+file+"?version=1",alice,"scope-a",null).status()).isEqualTo(409);
        // 同单位读者无需文件管理权限，附件读取沿父通知授权；解除后立即拒绝。
        membership("u-bob","scope-a",false);
        role("g1-notice-reader","scope-a","NOTICE_READER"); allow("g1-notice-reader","org-notice:read",false);
        jdbc.update("update sys_role set data_range=1 where id='g1-notice-reader'");
        jdbc.update("delete from sys_user_role where user_id='u-bob' and scope_id='scope-a'");
        grant("u-bob","g1-notice-reader","scope-a");
        assertThat(content(id,file,bob,"scope-a").body()).isEqualTo("attachment bytes".getBytes(StandardCharsets.UTF_8));
        assertThat(request("GET","/api/admin/files/"+file+"/content",bob,"scope-a",null).status()).isEqualTo(403);
        assertThat(request("GET",BASE+"/"+id+"/readers",bob,"scope-a",null).status()).isEqualTo(403);
        // 即使旧数据已进入回收站，永久删除仍重新验证有效引用。
        jdbc.update("update sys_file_entry set deleted=1 where id=?",file);
        assertThat(request("DELETE","/api/admin/files/recycle/"+file+"?version=1",alice,"scope-a",null).status()).isEqualTo(409);
        jdbc.update("update sys_file_entry set deleted=0 where id=?",file);
        assertThat(update(id,alice,0,List.of(),null).status()).isEqualTo(200);
        assertThat(content(id,file,bob,"scope-a").statusCode()).isEqualTo(404);
        String key=jdbc.queryForObject("select object_key from sys_file_entry where id=?",String.class,file);
        assertThat(Files.readString(root.resolve(key))).isEqualTo("attachment bytes");
        assertThat(request("DELETE","/api/admin/files/"+file+"?version=1",alice,"scope-a",null).status()).isEqualTo(200);
    }

    @Test void allAttachmentChoicesAreRecheckedAndInvalidBatchesRollback() throws Exception {
        String alice=token("alice"),bob=token("bob");
        String local=upload(alice,"scope-a","local.txt"),foreign=upload(bob,"scope-b","foreign.txt");
        var options=request("GET",BASE+"/create-files",alice,"scope-a",null);
        assertThat(options.status()).isEqualTo(200); assertThat(options.raw()).contains(local).doesNotContain(foreign);
        assertThat(create(alice,"scope-a","g1-a-root",List.of(local,foreign)).status()).isEqualTo(409);
        assertThat(count("select count(*) from sys_org_notice")).isZero();
        assertThat(count("select count(*) from sys_file_binding")).isZero();
        assertThat(create(alice,"scope-a","g1-a-root",List.of(local,local)).status()).isEqualTo(400);
        jdbc.update("update sys_file_entry set state='UPLOADING' where id=?",local);
        assertThat(create(alice,"scope-a","g1-a-root",List.of(local)).status()).isEqualTo(409);
        jdbc.update("update sys_file_entry set state='READY' where id=?",local);
        String id=create(alice,"scope-a","g1-a-root",List.of(local)).body().at("/data/notice/id").asText();
        assertThat(update(id,alice,0,List.of(foreign),"normal").status()).isEqualTo(409);
        assertThat(request("GET",BASE+"/"+id,alice,"scope-a",null).body().at("/data/attachments/0/id").asText()).isEqualTo(local);
        assertThat(request("DELETE",BASE+"/"+id+"?version=0",alice,"scope-a",null).status()).isEqualTo(200);
        assertThat(count("select count(*) from sys_file_binding")).isZero();
        assertThat(CLIENT.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/admin/files/"+local+"/content"))
                .header("Authorization","Bearer "+alice).header("X-Cyys-Scope-Id","scope-a").GET().build(),HttpResponse.BodyHandlers.ofByteArray()).statusCode()).isEqualTo(200);
    }

    @Test void dictionaryChangesShowHistoricalValueButCannotAssignDeletedTypes() throws Exception {
        String alice=token("alice");
        var choices=request("GET",BASE+"/create-options",alice,"scope-a",null);
        assertThat(choices.raw()).contains("一般通知");
        String id=create(alice,"scope-a","g1-a-root",List.of()).body().at("/data/notice/id").asText();
        jdbc.update("update param_basic_type set param_value='历史类别',deleted=1 where id='98102'");
        var detail=request("GET",BASE+"/"+id,alice,"scope-a",null);
        assertThat(detail.body().at("/data/notice/typeLabel").asText()).isEqualTo("历史类别");
        assertThat(detail.body().at("/data/notice/typeDisabled").asBoolean()).isTrue();
        assertThat(create(alice,"scope-a","g1-a-root",List.of()).status()).isEqualTo(409);
        assertThat(update(id,alice,0,List.of(),"normal").status()).isEqualTo(200);
        assertThat(update(id,alice,1,List.of(),null).status()).isEqualTo(200);
        assertThat(update(id,alice,2,List.of(),"normal").status()).isEqualTo(409);
    }

    @Test void concurrentRepeatedReadsRecordOnlyOneFirstReceiptAndProtectDetails() throws Exception {
        String alice=token("alice"),bob=token("bob");
        String id=create(alice,"scope-a","g1-a-root",List.of()).body().at("/data/notice/id").asText();
        try(var workers=Executors.newFixedThreadPool(2)) {
            var first=workers.submit(()->request("POST",BASE+"/"+id+"/read",alice,"scope-a",null));
            var second=workers.submit(()->request("POST",BASE+"/"+id+"/read",alice,"scope-a",null));
            assertThat(first.get(5,TimeUnit.SECONDS).status()).isEqualTo(200);
            assertThat(second.get(5,TimeUnit.SECONDS).status()).isEqualTo(200);
        }
        assertThat(count("select count(*) from sys_org_notice_ref")).isEqualTo(1);
        var list=request("GET",BASE,alice,"scope-a",null);
        assertThat(list.body().at("/data/records/0/readCount").asInt()).isEqualTo(1);
        assertThat(list.body().at("/data/records/0/read").asBoolean()).isTrue();
        assertThat(request("GET",BASE+"/"+id+"/readers",alice,"scope-a",null).body().at("/data/total").asInt()).isEqualTo(1);
        assertThat(request("GET",BASE+"/"+id+"/readers",bob,"scope-b",null).status()).isEqualTo(404);
        assertThat(update(id,alice,0,List.of(),"normal").status()).isEqualTo(200);
        assertThat(count("select count(*) from sys_org_notice_ref")).isEqualTo(1);
    }

    @Test void bindingAndFileDeleteSerializeWithoutLeavingAnInvalidReference() throws Exception {
        String alice=token("alice"); String file=upload(alice,"scope-a","race.txt");
        try(var workers=Executors.newFixedThreadPool(2)) {
            var bind=workers.submit(()->create(alice,"scope-a","g1-a-root",List.of(file)));
            var remove=workers.submit(()->request("DELETE","/api/admin/files/"+file+"?version=1",alice,"scope-a",null));
            int bound=bind.get(5,TimeUnit.SECONDS).status(),deleted=remove.get(5,TimeUnit.SECONDS).status();
            assertThat(List.of(bound,deleted)).containsExactlyInAnyOrder(200,409);
        }
        assertThat(count("select count(*) from sys_file_binding b join sys_file_entry f on f.id=b.file_id where f.deleted=1")).isZero();
    }

    private Reply create(String token,String scope,String department,List<String> files) throws Exception {
        return request("POST",BASE,token,scope,JSON.writeValueAsString(Map.of("departmentId",department,"title","通知标题","content","<script>plain text</script>","type","normal","attachments",files)));
    }
    private Reply update(String id,String token,int version,List<String> files,String type) throws Exception {
        Map<String,Object> body=new LinkedHashMap<>(); body.put("title","更新通知");body.put("content","通知内容");body.put("type",type);body.put("attachments",files);body.put("version",version);
        return request("PUT",BASE+"/"+id,token,"scope-a",JSON.writeValueAsString(body));
    }
    private String upload(String token,String scope,String name) throws Exception {
        String boundary="NoticeBoundary";
        String body="--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+name+"\"\r\nContent-Type: text/plain\r\n\r\nattachment bytes\r\n--"+boundary+"--\r\n";
        var result=send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/admin/files"))
                .header("Authorization","Bearer "+token).header("X-Cyys-Scope-Id",scope).header("Content-Type","multipart/form-data; boundary="+boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build());
        assertThat(result.status()).as(result.raw()).isEqualTo(200);
        return result.body().at("/data/id").asText();
    }
    private HttpResponse<byte[]> content(String notice,String file,String token,String scope) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+BASE+"/"+notice+"/attachments/"+file+"/content"))
                .header("Authorization","Bearer "+token).header("X-Cyys-Scope-Id",scope).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
    }
}
