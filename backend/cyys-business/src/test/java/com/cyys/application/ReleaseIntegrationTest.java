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
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"cyys.data-isolation.mode=multi","cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class ReleaseIntegrationTest extends IsolationTestSupport {
    private static final String ROOT="/api/admin/platform/releases";
    @Autowired StorageProperties storage;
    private Path root;

    @BeforeEach void releases() throws Exception {
        root=Files.createTempDirectory(Path.of("target").toAbsolutePath(),"release-files-");
        var profile=new StorageProperties.Profile(); profile.setDriver("local"); profile.setRoot(root.toString());
        storage.setActive("release"); storage.setProfiles(Map.of("release",profile));
        platformUser();
        for(String action:List.of("read","create","update","delete")) allow("role-platform","release:"+action,true);
        allow("role-platform","platform:data:operate",true);
        for(String role:List.of("role-a","role-b"))
            for(String action:List.of("read","upload","content:read","delete")) allow(role,"file:"+action,false);
        jdbc.update("insert into param_basic(id,name,system_name) values('98401','版本类型','VERSION_TYPE')");
        jdbc.update("insert into param_basic_type(id,basic_id,param_key,param_value) values('98402','98401','normal','常规更新')");
    }

    @AfterEach void clearReleases() throws Exception {
        jdbc.update("delete from sys_file_binding where scope_id='GLOBAL' and resource='RELEASE' and record_id in (select id from sys_version where created_by='u-platform')");
        jdbc.update("delete from sys_version where created_by='u-platform'");
        jdbc.update("delete from sys_data_audit where actor_id='u-platform' and resource='release'");
        for(String key:jdbc.queryForList("select object_key from sys_file_entry where scope_id in ('scope-a','scope-b')",String.class)) {
            assertThat(key).matches("[a-f0-9]{32}"); Files.deleteIfExists(root.resolve(key));
        }
        jdbc.update("delete from sys_file_entry where scope_id in ('scope-a','scope-b')");
        jdbc.update("delete from param_basic_type where basic_id='98401'");
        jdbc.update("delete from param_basic where id='98401'");
        Files.delete(root);
    }

    @Test void sharedVersionsKeepStrictDatesHistoricalTypesAndOptimisticVersions() throws Exception {
        String admin=token("platform");
        String id=create(admin,null,List.of());
        for(String scope:List.of("scope-a","scope-b"))
            assertThat(request("GET",ROOT+"?number=1.0",admin,scope,null).body().at("/data/records/0/id").asText()).isEqualTo(id);
        assertThat(request("POST",ROOT,admin,null,command(null,List.of()).replace("2026-09-20","2026-02-30")).status()).isEqualTo(400);
        jdbc.update("update param_basic_type set deleted=1 where id='98402'");
        assertThat(request("GET",ROOT+"/"+id,admin,null,null).body().at("/data/release/typeDisabled").asBoolean()).isTrue();
        assertThat(update(admin,null,id,0,List.of()).status()).isEqualTo(200);
        assertThat(update(admin,null,id,0,List.of()).status()).isEqualTo(409);
        assertThat(request("POST",ROOT,admin,null,command(null,List.of())).status()).isEqualTo(409);
        assertThat(request("DELETE",ROOT+"/"+id+"?version=1",admin,null,null).status()).isEqualTo(200);
        assertThat(request("GET",ROOT+"/"+id,admin,null,null).status()).isEqualTo(404);
    }

    @Test void sharedAttachmentUsesParentAuthorizationAndUnlinkKeepsOriginalObject() throws Exception {
        String admin=token("platform"),alice=token("alice"),file=upload(alice,"scope-a");
        String id=create(admin,"scope-a",List.of(file));
        assertThat(jdbc.queryForObject("select file_scope_id from sys_file_binding where record_id=?",String.class,id)).isEqualTo("scope-a");
        assertThat(download(admin,"scope-b",id,file).body()).isEqualTo("release bytes".getBytes(StandardCharsets.UTF_8));
        assertThat(request("GET","/api/admin/files/"+file+"/content",admin,"scope-b",null).status()).isEqualTo(403);
        assertThat(request("DELETE","/api/admin/files/"+file+"?version=1",alice,"scope-a",null).status()).isEqualTo(409);
        assertThat(update(admin,"scope-b",id,0,List.of(file)).status()).isEqualTo(200);
        assertThat(update(admin,"scope-b",id,1,List.of()).status()).isEqualTo(200);
        assertThat(download(admin,"scope-b",id,file).statusCode()).isEqualTo(404);
        String key=jdbc.queryForObject("select object_key from sys_file_entry where id=?",String.class,file);
        assertThat(Files.readString(root.resolve(key))).isEqualTo("release bytes");
        assertThat(request("DELETE","/api/admin/files/"+file+"?version=1",alice,"scope-a",null).status()).isEqualTo(200);
    }

    @Test void newBindingsRequireSelectedAuthorizedUnitAndPermissionRevocationBlocksDownload() throws Exception {
        String admin=token("platform"),file=upload(token("alice"),"scope-a");
        assertThat(request("POST",ROOT,admin,"scope-b",command(null,List.of(file))).status()).isEqualTo(409);
        assertThat(count("select count(*) from sys_version where created_by='u-platform'")).isZero();
        String id=create(admin,"scope-a",List.of(file));
        jdbc.update("update sys_menu set status=0 where code='release:read'");
        assertThat(download(admin,"scope-b",id,file).statusCode()).isEqualTo(403);
        jdbc.update("update sys_menu set status=1 where code='release:read'");
        jdbc.update("update sys_menu set status=0 where code='platform:data:operate'");
        assertThat(request("POST",ROOT,admin,"scope-a",command(null,List.of(file))).status()).isEqualTo(403);
        assertThat(update(admin,"scope-b",id,0,List.of(file)).status()).isEqualTo(200);
        assertThat(download(token("bob"),"scope-b",id,file).statusCode()).isEqualTo(403);
    }

    @Test void concurrentBindingAndFileDeletionNeverLeaveBrokenSharedReference() throws Exception {
        String admin=token("platform"),alice=token("alice"),file=upload(alice,"scope-a");
        try(var workers=Executors.newFixedThreadPool(2)) {
            var binding=workers.submit(() -> request("POST",ROOT,admin,"scope-a",command(null,List.of(file))));
            var deletion=workers.submit(() -> request("DELETE","/api/admin/files/"+file+"?version=1",alice,"scope-a",null));
            assertThat(List.of(binding.get().status(),deletion.get().status())).containsExactlyInAnyOrder(200,409);
        }
        assertThat(count("select count(*) from sys_file_binding b join sys_file_entry f on f.id=b.file_id where b.resource='RELEASE' and f.deleted=1")).isZero();
    }

    private String create(String token,String scope,List<String> files) throws Exception {
        var reply=request("POST",ROOT,token,scope,command(null,files));
        assertThat(reply.status()).as(reply.raw()).isEqualTo(200);
        return reply.body().at("/data/release/id").asText();
    }
    private Reply update(String token,String scope,String id,int version,List<String> files) throws Exception {
        return request("PUT",ROOT+"/"+id,token,scope,command(version,files));
    }
    private String command(Integer version,List<String> files) throws Exception {
        var body=new LinkedHashMap<String,Object>(Map.of("releaseNumber","1.0","dateUpdate","2026-09-20","type","normal","content","更新说明\n<script>文本</script>","attachments",files));
        if(version!=null) body.put("version",version); return JSON.writeValueAsString(body);
    }
    private String upload(String token,String scope) throws Exception {
        String boundary="ReleaseBoundary";
        String body="--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"release.txt\"\r\nContent-Type: text/plain\r\n\r\nrelease bytes\r\n--"+boundary+"--\r\n";
        var reply=send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/admin/files"))
                .header("Authorization","Bearer "+token).header("X-Cyys-Scope-Id",scope).header("Content-Type","multipart/form-data; boundary="+boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build());
        assertThat(reply.status()).as(reply.raw()).isEqualTo(200);
        return reply.body().at("/data/id").asText();
    }
    private HttpResponse<byte[]> download(String token,String scope,String release,String file) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+ROOT+"/"+release+"/attachments/"+file))
                .header("Authorization","Bearer "+token).header("X-Cyys-Scope-Id",scope).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
    }
}
