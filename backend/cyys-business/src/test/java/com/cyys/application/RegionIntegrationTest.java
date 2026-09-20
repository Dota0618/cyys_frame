package com.cyys.application;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
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
import java.util.*;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class, webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"cyys.data-isolation.mode=multi","cyys.data-isolation.single-scope-id="})
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class RegionIntegrationTest extends HttpTestSupport {
    private static final String ROOT="/api/admin/platform/regions";
    @BeforeEach void regions() {
        platformUser();
        for(String action:List.of("read","create","update","delete","export"))
            permission("role-platform","g1-region-"+action,"region:"+action,true);
    }
    @AfterEach void clearRegions() {
        jdbc.update("delete from param_region where created_by='u-platform'");
        jdbc.update("delete from sys_data_audit where actor_id='u-platform' and resource='region'");
    }

    @Test void hierarchyVersionAndWholeBatchValidationPreserveExistingRecords() throws Exception {
        String admin=token("platform");
        String parent=create(admin,null,"000001","国家甲"), child=create(admin,parent,"000002","地区甲");
        assertThat(request("GET",ROOT+"/"+child,admin,"scope-b",null).body().at("/data/level").asInt()).isEqualTo(1);
        assertThat(request("GET",ROOT+"?parentId="+parent,admin,null,null).body().at("/data/total").asInt()).isEqualTo(1);
        assertThat(request("DELETE",ROOT+"/"+parent+"?version=0",admin,null,null).status()).isEqualTo(409);
        String other=create(admin,null,"000003","国家乙");
        var batch=request("POST",ROOT+"/delete",admin,null,JSON.writeValueAsString(List.of(
                Map.of("id",other,"version",0),Map.of("id",parent,"version",0))));
        assertThat(batch.status()).isEqualTo(409);
        assertThat(request("GET",ROOT+"/"+other,admin,null,null).status()).isEqualTo(200);
        var edit=new LinkedHashMap<String,Object>(Map.of("version",0,"cityCode","000002","name","修改地区","sort",1));
        edit.put("longitude",null); edit.put("latitude",null);
        assertThat(request("PUT",ROOT+"/"+child,admin,null,JSON.writeValueAsString(edit)).status()).isEqualTo(200);
        var cleared=request("GET",ROOT+"/"+child,admin,null,null).body().at("/data/longitude");
        assertThat(cleared.isNull() || cleared.isMissingNode()).isTrue();
        assertThat(jdbc.queryForObject("select longitude from param_region where id=?",java.math.BigDecimal.class,child)).isNull();
        assertThat(request("PUT",ROOT+"/"+child,admin,null,JSON.writeValueAsString(edit)).status()).isEqualTo(409);
        assertThat(request("DELETE",ROOT+"/"+child+"?version=1",admin,null,null).status()).isEqualTo(200);
        assertThat(request("POST",ROOT,admin,null,region(null,"000002","重用历史编码")).status()).isEqualTo(409);
        assertThat(request("DELETE",ROOT+"/"+parent+"?version=0",admin,null,null).status()).isEqualTo(200);
    }

    @Test void parentDeleteAndChildCreateCannotLeaveLiveOrphan() throws Exception {
        String admin=token("platform"),parent=create(admin,null,"000001","并发父级");
        try(var workers=Executors.newFixedThreadPool(2)) {
            var created=workers.submit(() -> request("POST",ROOT,admin,null,region(parent,"000002","并发子级")));
            var deleted=workers.submit(() -> request("DELETE",ROOT+"/"+parent+"?version=0",admin,null,null));
            var statuses=List.of(created.get().status(),deleted.get().status());
            assertThat(statuses.equals(List.of(200,409)) || statuses.equals(List.of(404,200)))
                    .as("create/delete outcomes: %s",statuses).isTrue();
        }
        assertThat(jdbc.queryForObject("select count(*) from param_region c join param_region p on p.id=c.parent_id where c.deleted=0 and p.deleted=1",Integer.class)).isZero();
    }

    @Test void exportUsesCurrentLevelAndKeepsCodesAndFormulaTextAsStrings() throws Exception {
        String admin=token("platform");
        String parent=create(admin,null,"000001","=1+1");
        create(admin,parent,"000002","不包含下级");
        var reply=export(admin,"{}");
        assertThat(reply.statusCode()).isEqualTo(200);
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(reply.body()))) {
            var sheet=book.getSheetAt(0);
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(sheet.getRow(1).getCell(0).getCellType()).isEqualTo(CellType.STRING);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("=1+1");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("000001");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("001234");
        }
        try(var empty=new XSSFWorkbook(new ByteArrayInputStream(export(admin,"{\"name\":\"不存在\"}").body()))) {
            assertThat(empty.getSheetAt(0).getLastRowNum()).isZero();
        }
        jdbc.update("delete from sys_role_menu where role_id='role-platform' and menu_id='g1-region-export'");
        assertThat(request("GET",ROOT,admin,null,null).status()).isEqualTo(200);
        assertThat(export(admin,"{}").statusCode()).isEqualTo(403);
        jdbc.update("insert into sys_role_menu(id,role_id,menu_id) values('g1-region-unit','role-a','g1-region-export')");
        assertThat(export(token("alice"),"{}").statusCode()).isEqualTo(403);
    }

    @Test void exportRejectsOverLimitInsteadOfSilentlyTruncating() throws Exception {
        var values=new ArrayList<Object[]>();
        for(int i=0;i<5000;i++) values.add(new Object[]{"g1-region-"+i,"bulk-"+i,"批量地区"});
        jdbc.batchUpdate("insert into param_region(id,city_code,name,level,created_by,updated_by) values(?,?,?,0,'u-platform','u-platform')",values);
        String admin=token("platform");
        var full=export(admin,"{}");
        assertThat(full.statusCode()).isEqualTo(200);
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(full.body()))) {
            assertThat(book.getSheetAt(0).getLastRowNum()).isEqualTo(5000);
        }
        create(admin,null,"overflow","超限");
        assertThat(export(admin,"{}").statusCode()).isEqualTo(422);
    }

    private String create(String token,String parent,String code,String name) throws Exception {
        var reply=request("POST",ROOT,token,null,region(parent,code,name));
        assertThat(reply.status()).as(reply.raw()).isEqualTo(200);
        return reply.body().at("/data/id").asText();
    }
    private String region(String parent,String code,String name) throws Exception {
        var body=new LinkedHashMap<String,Object>(Map.of("cityCode",code,"name",name,"zipCode","001234","longitude",120.5,"latitude",30.5));
        body.put("parentId",parent); return JSON.writeValueAsString(body);
    }
    private HttpResponse<byte[]> export(String token,String body) throws Exception {
        return CLIENT.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+ROOT+"/export"))
                .timeout(Duration.ofSeconds(30)).header("Authorization","Bearer "+token)
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofByteArray());
    }
}
