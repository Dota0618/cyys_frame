package com.cyys.application;

import com.cyys.common.storage.StorageProperties;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"cyys.data-isolation.mode=multi", "cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class FileIntegrationTest extends IsolationTestSupport {
    private static final String BASE_FILE = "/api/admin/files";
    @Autowired StorageProperties storage;
    private Path root;
    private Path second;

    @BeforeEach void setupFiles() throws Exception {
        Path target = Path.of("target").toAbsolutePath().normalize();
        root = Files.createTempDirectory(target, "file-test-");
        second = Files.createTempDirectory(target, "file-next-");
        for (String action : List.of("read","upload","content:read","update","delete")) {
            allow("role-a", "file:" + action, false); allow("role-b", "file:" + action, false);
        }
        for(String action:List.of("move","directory:create","directory:update","directory:delete","recycle:read","restore","purge")) {
            allow("role-a","file:"+action,false); allow("role-b","file:"+action,false);
        }
        StorageProperties.Profile first = new StorageProperties.Profile(); first.setDriver("local"); first.setRoot(root.toString());
        StorageProperties.Profile next = new StorageProperties.Profile(); next.setDriver("local"); next.setRoot(second.toString());
        StorageProperties.Profile remote = new StorageProperties.Profile(); remote.setDriver("s3");
        storage.setProfiles(new HashMap<>(Map.of("first",first,"next",next,"remote",remote)));
        storage.setActive("first"); storage.setMaxBytes(4096); storage.setPartBytes(1024); storage.setMaxResumableBytes(8192);
    }

    @AfterEach void cleanupFiles() throws Exception {
        // 仅删除本测试元数据登记的键；临时根目录必须为空才能移除。
        for (String table : List.of("sys_file_part", "sys_file_entry")) {
            for (String key : jdbc.queryForList("select object_key from " + table + " where scope_id in ('scope-a','scope-b')", String.class)) {
                assertThat(key).matches("[a-f0-9]{32}");
                if (root != null) Files.deleteIfExists(root.resolve(key));
                if (second != null) Files.deleteIfExists(second.resolve(key));
            }
            jdbc.update("delete from " + table + " where scope_id in ('scope-a','scope-b')");
        }
        if (root != null) Files.delete(root);
        if (second != null) Files.delete(second);
        jdbc.update("delete from sys_file_dir where scope_id in ('scope-a','scope-b')");
    }

    @Test void directUploadIsPrivateVersionedAndKeepsHistoricalProfile() throws Exception {
        String alice=token("alice"), bob=token("bob");
        byte[] data = "中文文件\ncontent".getBytes(StandardCharsets.UTF_8);
        Reply uploaded = multipart("POST", BASE_FILE, alice, "scope-a", "报告.txt", data);
        assertThat(uploaded.status()).as(uploaded.raw()).isEqualTo(200);
        String id=uploaded.body().at("/data/id").asText();
        assertThat(uploaded.raw()).doesNotContain(root.toString(),"storageProfile","objectKey","sha256");
        assertThat(download(id,alice,"scope-a").body()).isEqualTo(data);
        assertThat(download(id,bob,"scope-b").statusCode()).isEqualTo(404);
        assertThat(download(id,alice,"scope-b").statusCode()).isEqualTo(403);
        storage.setActive("next");
        assertThat(download(id,alice,"scope-a").body()).isEqualTo(data);
        assertThat(download(id,alice,"scope-a").headers().firstValue("Cache-Control")).contains("no-store");
        int version=uploaded.body().at("/data/version").asInt();
        assertThat(request("PUT",BASE_FILE+"/"+id,alice,"scope-a",JSON.writeValueAsString(Map.of("name","重命名.txt","version",version))).status()).isEqualTo(200);
        assertThat(request("DELETE",BASE_FILE+"/"+id+"?version="+version,alice,"scope-a",null).status()).isEqualTo(409);
        assertThat(request("DELETE",BASE_FILE+"/"+id+"?version="+(version+1),alice,"scope-a",null).status()).isEqualTo(200);
        assertThat(download(id,alice,"scope-a").statusCode()).isEqualTo(404);
        try(var objects=Files.list(root)) { assertThat(objects.count()).isEqualTo(1); } // 逻辑删除保留回收可能。
    }

    @Test void resumablePartsSurviveNewSessionAndMergeExactlyOnce() throws Exception {
        String alice=token("alice"); byte[] bytes = data(2500);
        String id=start(alice,bytes);
        assertThat(part(id,0,alice,Arrays.copyOfRange(bytes,0,1024)).status()).isEqualTo(200);
        assertThat(part(id,0,alice,Arrays.copyOfRange(bytes,0,1024)).status()).isEqualTo(200);
        assertThat(request("POST",BASE_FILE+"/uploads/"+id+"/complete",alice,"scope-a",null).status()).isEqualTo(409);
        assertThat(download(id,alice,"scope-a").statusCode()).isEqualTo(409);
        // 新登录与新 HTTP 请求读取持久化进度；切换新写入目标不影响旧上传任务。
        String resumed=token("alice"); storage.setActive("next");
        Reply progress=request("GET",BASE_FILE+"/uploads/"+id,resumed,"scope-a",null);
        assertThat(progress.body().at("/data/completed").toString()).isEqualTo("[0]");
        assertThat(part(id,2,resumed,Arrays.copyOfRange(bytes,2048,2500)).status()).isEqualTo(200);
        assertThat(part(id,1,resumed,Arrays.copyOfRange(bytes,1024,2048)).status()).isEqualTo(200);
        for(int retry=0;retry<2;retry++) assertThat(request("POST",BASE_FILE+"/uploads/"+id+"/complete",resumed,"scope-a",null).status()).isEqualTo(200);
        assertThat(download(id,resumed,"scope-a").body()).isEqualTo(bytes);
        assertThat(count("select count(*) from sys_data_audit where resource='file' and operation='UPLOAD'")).isEqualTo(1);
        assertThat(request("POST",BASE_FILE+"/uploads/"+id+"/cleanup",resumed,"scope-a",null).status()).isEqualTo(200);
        try(var first=Files.list(root); var next=Files.list(second)) { assertThat(first.count()).isEqualTo(1); assertThat(next.count()).isZero(); }
    }

    @Test void invalidChunksExpiredTasksRevocationAndOtherActorsCannotResume() throws Exception {
        String alice=token("alice"), bob=token("bob"); byte[] bytes=data(1300); String id=start(alice,bytes);
        assertThat(part(id,0,alice,new byte[1024]).status()).isEqualTo(422);
        assertThat(part(id,0,alice,new byte[12]).status()).isEqualTo(400);
        assertThat(part(id,5,alice,new byte[1024]).status()).isEqualTo(400);
        assertThat(request("GET",BASE_FILE+"/uploads/"+id,bob,"scope-b",null).status()).isEqualTo(404);
        membership("u-bob","scope-a",false); grant("u-bob","role-a","scope-a");
        assertThat(request("GET",BASE_FILE+"/uploads/"+id,bob,"scope-a",null).status()).isEqualTo(404);
        jdbc.update("update sys_menu set status=0 where code='file:upload'");
        assertThat(part(id,0,alice,Arrays.copyOfRange(bytes,0,1024)).status()).isEqualTo(403);
        jdbc.update("update sys_menu set status=1 where code='file:upload'");
        jdbc.update("update sys_file_entry set expires_at=DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 1 DAY) where id=?",id);
        assertThat(part(id,0,alice,Arrays.copyOfRange(bytes,0,1024)).status()).isEqualTo(410);
        assertThat(request("POST",BASE_FILE+"/uploads/"+id+"/cancel",alice,"scope-a",null).status()).isEqualTo(200);
        assertThat(part(id,0,alice,Arrays.copyOfRange(bytes,0,1024)).status()).isEqualTo(409);
        try(var objects=Files.list(root)) { assertThat(objects.count()).isZero(); }
    }

    @Test void corruptedDiskPartNeverProducesReadyFileAndCanBeRepaired() throws Exception {
        String alice=token("alice"); byte[] bytes=data(512); String id=start(alice,bytes);
        assertThat(part(id,0,alice,bytes).status()).isEqualTo(200);
        String key=jdbc.queryForObject("select object_key from sys_file_part where file_id=?",String.class,id);
        Files.write(root.resolve(key),new byte[512]);
        assertThat(request("POST",BASE_FILE+"/uploads/"+id+"/complete",alice,"scope-a",null).status()).isEqualTo(422);
        assertThat(download(id,alice,"scope-a").statusCode()).isEqualTo(409);
        assertThat(count("select count(*) from sys_data_audit where resource='file' and operation='UPLOAD'")).isZero();
        assertThat(request("GET",BASE_FILE+"/uploads/"+id,alice,"scope-a",null).body().at("/data/completed").isEmpty()).isTrue();
        assertThat(part(id,0,alice,bytes).status()).isEqualTo(200);
        assertThat(request("POST",BASE_FILE+"/uploads/"+id+"/complete",alice,"scope-a",null).status()).isEqualTo(200);
    }

    @Test void limitsPathNamesEmptyFilesAndUnimplementedDriversFailExplicitly() throws Exception {
        String alice=token("alice");
        assertThat(multipart("POST",BASE_FILE,alice,"scope-a","../escape",data(10)).status()).isEqualTo(400);
        assertThat(multipart("POST",BASE_FILE,alice,"scope-a","big.bin",data(4097)).status()).isEqualTo(413);
        assertThat(multipart("POST",BASE_FILE,alice,"scope-a","empty.bin",new byte[0]).status()).isEqualTo(200);
        storage.setActive("remote");
        assertThat(multipart("POST",BASE_FILE,alice,"scope-a","test.bin",data(10)).status()).isEqualTo(503);
        assertThat(count("select count(*) from sys_file_entry")).isEqualTo(1);
        assertThat(request("GET",BASE_FILE+"?page=-1",alice,"scope-a",null).status()).isEqualTo(400);
    }

    @Test void sameUnitDepartmentAndSelfRangesApplyToFilesAndDepartmentDeletion() throws Exception {
        membership("u-bob","scope-a",false); grant("u-bob","role-a","scope-a");
        jdbc.update("update sys_user_scope set org_id='g1-a-child' where user_id='u-bob' and scope_id='scope-a'");
        jdbc.update("update sys_role set data_range=3 where id='role-a'");
        String alice=token("alice"), bob=token("bob");
        Reply created=multipart("POST",BASE_FILE,bob,"scope-a","department.bin",data(12));
        assertThat(created.status()).as(created.raw()).isEqualTo(200);
        String id=created.body().at("/data/id").asText();
        assertThat(download(id,alice,"scope-a").statusCode()).isEqualTo(404);
        jdbc.update("update sys_role set data_range=4 where id='role-a'");
        assertThat(download(id,alice,"scope-a").statusCode()).isEqualTo(200);
        jdbc.update("update sys_role set data_range=5 where id='role-a'");
        assertThat(download(id,alice,"scope-a").statusCode()).isEqualTo(404);
        assertThat(download(id,bob,"scope-a").statusCode()).isEqualTo(200);
        jdbc.update("update sys_role set data_range=1 where id='role-a'");
        jdbc.update("update sys_user_scope set org_id=null where user_id='u-bob' and scope_id='scope-a'");
        assertThat(request("DELETE","/api/admin/departments",alice,"scope-a","[{\"id\":\"g1-a-child\",\"version\":0}]").status()).isEqualTo(409);
        jdbc.update("update sys_menu set status=0 where code='file:content:read'");
        assertThat(download(id,alice,"scope-a").statusCode()).isEqualTo(403);
    }

    @Test void concurrentRetriesSerializePartsAndCompletionWithoutDuplicatingFiles() throws Exception {
        String alice=token("alice"); byte[] bytes=data(500); String id=start(alice,bytes);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first=pool.submit(() -> part(id,0,alice,bytes));
            var second=pool.submit(() -> part(id,0,alice,bytes));
            assertThat(first.get().status()).isEqualTo(200); assertThat(second.get().status()).isEqualTo(200);
            var completeA=pool.submit(() -> request("POST",BASE_FILE+"/uploads/"+id+"/complete",alice,"scope-a",null));
            var completeB=pool.submit(() -> request("POST",BASE_FILE+"/uploads/"+id+"/complete",alice,"scope-a",null));
            assertThat(completeA.get().status()).isEqualTo(200); assertThat(completeB.get().status()).isEqualTo(200);
        }
        assertThat(count("select count(*) from sys_data_audit where resource='file' and operation='UPLOAD'")).isEqualTo(1);
        assertThat(download(id,alice,"scope-a").body()).isEqualTo(bytes);
        int version=jdbc.queryForObject("select version from sys_file_entry where id=?",Integer.class,id);
        assertThat(request("DELETE",BASE_FILE+"/"+id+"?version="+version,alice,"scope-a",null).status()).isEqualTo(200);
        try(var objects=Files.list(root)) { assertThat(objects.count()).isEqualTo(1); } // 保留主文件，移除所有分片。
    }

    @Test void personalDirectoriesMoveCompleteBatchesAndDeleteBackToDefaultWithoutDeletingFiles() throws Exception {
        String alice=token("alice"),bob=token("bob");
        Reply directory=request("POST","/api/admin/file-directories",alice,"scope-a","{\"name\":\"工作资料\"}");
        assertThat(directory.status()).as(directory.raw()).isEqualTo(200);
        String dir=directory.body().at("/data/id").asText();
        var uploaded=multipart("POST",BASE_FILE,alice,"scope-a","move.bin",data(10));
        String id=uploaded.body().at("/data/id").asText(); int version=uploaded.body().at("/data/version").asInt();
        String moving=JSON.writeValueAsString(Map.of("directoryId",dir,"files",List.of(Map.of("id",id,"version",version))));
        assertThat(request("POST",BASE_FILE+"/move",alice,"scope-a",moving).status()).isEqualTo(200);
        assertThat(request("GET",BASE_FILE+"?directoryId="+dir,alice,"scope-a",null).body().at("/data/total").asInt()).isEqualTo(1);
        assertThat(request("GET",BASE_FILE+"?directoryId=__root__",alice,"scope-a",null).body().at("/data/total").asInt()).isZero();
        membership("u-bob","scope-a",false); grant("u-bob","role-a","scope-a");
        assertThat(request("GET",BASE_FILE+"?directoryId="+dir,bob,"scope-a",null).status()).isEqualTo(404);
        assertThat(request("GET","/api/admin/file-directories",bob,"scope-a",null).body().at("/data").isEmpty()).isTrue();
        assertThat(request("DELETE","/api/admin/file-directories/"+dir+"?version=0",bob,"scope-a",null).status()).isEqualTo(404);
        assertThat(request("DELETE","/api/admin/file-directories/"+dir+"?version=0",alice,"scope-a",null).status()).isEqualTo(200);
        assertThat(request("GET",BASE_FILE+"?directoryId=__root__",alice,"scope-a",null).body().at("/data/total").asInt()).isEqualTo(1);
        assertThat(download(id,alice,"scope-a").body()).isEqualTo(data(10));
    }

    @Test void mixedUnitAndStaleBatchesNeverPartiallyMoveTrashOrRestore() throws Exception {
        String alice=token("alice"),bob=token("bob");
        var a=multipart("POST",BASE_FILE,alice,"scope-a","a.bin",data(5)).body().at("/data");
        var b=multipart("POST",BASE_FILE,bob,"scope-b","b.bin",data(5)).body().at("/data");
        var refs=List.of(Map.of("id",a.path("id").asText(),"version",a.path("version").asInt()),Map.of("id",b.path("id").asText(),"version",b.path("version").asInt()));
        assertThat(request("POST",BASE_FILE+"/move",alice,"scope-a",JSON.writeValueAsString(Map.of("files",refs))).status()).isEqualTo(409);
        assertThat(request("POST",BASE_FILE+"/trash",alice,"scope-a",JSON.writeValueAsString(refs)).status()).isEqualTo(409);
        assertThat(download(a.path("id").asText(),alice,"scope-a").statusCode()).isEqualTo(200);
        assertThat(request("DELETE",BASE_FILE+"/"+a.path("id").asText()+"?version="+a.path("version").asInt(),alice,"scope-a",null).status()).isEqualTo(200);
        assertThat(request("POST",BASE_FILE+"/restore",alice,"scope-a",JSON.writeValueAsString(refs)).status()).isEqualTo(409);
        assertThat(download(a.path("id").asText(),alice,"scope-a").statusCode()).isEqualTo(404);
    }

    @Test void recycleRestoreAndPermanentDeleteRespectStatePermissionsAndFailureRecovery() throws Exception {
        String alice=token("alice"),bob=token("bob");
        var uploaded=multipart("POST",BASE_FILE,alice,"scope-a","recycle.bin",data(12)).body().at("/data");
        String id=uploaded.path("id").asText(); int version=uploaded.path("version").asInt();
        assertThat(request("DELETE",BASE_FILE+"/"+id+"?version="+version,alice,"scope-a",null).status()).isEqualTo(200);
        var listed=request("GET",BASE_FILE+"/recycle?search=recycle",alice,"scope-a",null);
        assertThat(listed.status()).as(listed.raw()).isEqualTo(200);
        assertThat(listed.body().at("/data/total").asInt()).isEqualTo(1);
        assertThat(request("GET",BASE_FILE+"/recycle",bob,"scope-b",null).body().at("/data/total").asInt()).isZero();
        version++;
        assertThat(request("POST",BASE_FILE+"/restore",alice,"scope-a",JSON.writeValueAsString(List.of(Map.of("id",id,"version",version)))).status()).isEqualTo(200);
        assertThat(download(id,alice,"scope-a").body()).isEqualTo(data(12));
        assertThat(request("DELETE",BASE_FILE+"/"+id+"?version="+(version+1),alice,"scope-a",null).status()).isEqualTo(200);
        version+=2;
        jdbc.update("update sys_menu set status=0 where code='file:purge'");
        assertThat(request("DELETE",BASE_FILE+"/recycle/"+id+"?version="+version,alice,"scope-a",null).status()).isEqualTo(403);
        jdbc.update("update sys_menu set status=1 where code='file:purge'");
        String key=jdbc.queryForObject("select object_key from sys_file_entry where id=?",String.class,id);
        // 用同名目录注入删除失败；本地驱动必须拒绝，不能标记永久删除成功。
        Files.delete(root.resolve(key)); Files.createDirectory(root.resolve(key));
        assertThat(request("DELETE",BASE_FILE+"/recycle/"+id+"?version="+version,alice,"scope-a",null).status()).isEqualTo(503);
        assertThat(jdbc.queryForObject("select state from sys_file_entry where id=?",String.class,id)).isEqualTo("PURGING");
        assertThat(count("select count(*) from sys_data_audit where resource='file' and operation='PURGE'")).isZero();
        assertThat(request("POST",BASE_FILE+"/restore",alice,"scope-a",JSON.writeValueAsString(List.of(Map.of("id",id,"version",version+1)))).status()).isEqualTo(409);
        Files.delete(root.resolve(key));
        assertThat(request("DELETE",BASE_FILE+"/recycle/"+id+"?version="+(version+1),alice,"scope-a",null).status()).isEqualTo(200);
        assertThat(request("GET",BASE_FILE+"/recycle",alice,"scope-a",null).body().at("/data/total").asInt()).isZero();
        assertThat(count("select count(*) from sys_data_audit where resource='file' and operation='PURGE'")).isEqualTo(1);
    }

    private String start(String token,byte[] bytes) throws Exception {
        List<String> hashes=new ArrayList<>();
        for(int offset=0;offset<bytes.length;offset+=1024) hashes.add(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Arrays.copyOfRange(bytes,offset,Math.min(bytes.length,offset+1024)))));
        Reply result=request("POST",BASE_FILE+"/uploads",token,"scope-a",JSON.writeValueAsString(Map.of("name","resume.bin","bytes",bytes.length,"hashes",hashes)));
        assertThat(result.status()).as(result.raw()).isEqualTo(200); return result.body().at("/data/id").asText();
    }
    private Reply part(String id,int index,String token,byte[] bytes) throws Exception { return multipart("PUT",BASE_FILE+"/uploads/"+id+"/parts/"+index,token,"scope-a","part",bytes); }
    private Reply multipart(String method,String path,String token,String scope,String name,byte[] bytes) throws Exception {
        String boundary="CyysTestBoundary";
        var output=new ByteArrayOutputStream();
        output.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\""+name+"\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(bytes); output.write(("\r\n--"+boundary+"--\r\n").getBytes(StandardCharsets.UTF_8));
        return send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).header("Authorization","Bearer "+token)
                .header("X-Cyys-Scope-Id",scope).header("Content-Type","multipart/form-data; boundary="+boundary)
                .method(method,HttpRequest.BodyPublishers.ofByteArray(output.toByteArray())).build());
    }
    private HttpResponse<byte[]> download(String id,String token,String scope) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+BASE_FILE+"/"+id+"/content"))
                .header("Authorization","Bearer "+token).header("X-Cyys-Scope-Id",scope).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
    }
    private static byte[] data(int size) { byte[] bytes=new byte[size]; new Random(42).nextBytes(bytes); return bytes; }
}
