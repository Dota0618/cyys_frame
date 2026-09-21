package com.cyys.application;

import com.cyys.admin.cache.service.BusinessCacheGroups;
import com.cyys.admin.department.mapper.DepartmentDisplayMapper;
import com.cyys.common.cache.CacheKeys;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.*;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties="cyys.environment=member-display-cache-acceptance")
class MemberDisplayCacheIntegrationTest extends HttpTestSupport {
    @Autowired CacheKeys keys;
    @Autowired StringRedisTemplate redis;
    @MockitoSpyBean DepartmentDisplayMapper departments;

    @BeforeEach void displayFixtures() {
        clearCaches();
        permission("role-a","g1-display-read","user:read",false);
        permission("role-a","g1-display-export","user:export",false);
        jdbc.update("insert into sys_org(id,scope_id,name,code,created_by) values "
                + "('g4-display-a','scope-a','部门甲','display-a','u-alice'),"
                + "('g4-display-b','scope-b','停用部门乙','display-b','u-bob')");
        jdbc.update("update sys_user_scope set data_range=0,org_id='g4-display-a' where user_id='u-alice'");
        jdbc.update("update sys_user_scope set org_id='g4-display-b',status=0 where user_id='u-bob'");
        jdbc.update("update sys_user set status=0 where id='u-bob'");
        jdbc.update("update sys_org set status=0 where id='g4-display-b'");
        clearInvocations(departments);
    }
    @AfterEach void clearCaches() {
        redis.delete(List.of(keys.platform(BusinessCacheGroups.SCOPE_NAMES.id()),
                keys.unit("scope-a",BusinessCacheGroups.DEPARTMENT_NAMES.id()),
                keys.unit("scope-b",BusinessCacheGroups.DEPARTMENT_NAMES.id())));
    }

    @Test void listAndDetailUseEachMembersActualUnitIncludingInactiveDepartments() throws Exception {
        String token=token("alice");
        var all=request("GET","/api/admin/users/all",token,null,null);
        assertThat(all.status()).as(all.raw()).isEqualTo(200);
        assertThat(all.body().at("/data/total").asInt()).isEqualTo(2);
        for(var row:all.body().at("/data/records")) {
            boolean alice=row.path("userId").asText().equals("u-alice");
            assertThat(row.path("scopeName").asText()).isEqualTo(alice?"总单位 A":"单位 B");
            assertThat(row.path("orgName").asText()).isEqualTo(alice?"部门甲":"停用部门乙");
        }
        var detail=request("GET","/api/admin/users/u-bob-scope-b",token,"scope-b",null);
        assertThat(detail.status()).as(detail.raw()).isEqualTo(200);
        assertThat(detail.body().at("/data/member/scopeName").asText()).isEqualTo("单位 B");
        assertThat(detail.body().at("/data/member/orgName").asText()).isEqualTo("停用部门乙");
        verify(departments,times(1)).labels("scope-a",List.of("g4-display-a"));
        verify(departments,times(1)).labels("scope-b",List.of("g4-display-b"));
    }

    @Test void exportReusesCachedDepartmentNameAndStillRecordsAudit() throws Exception {
        String token=token("alice");
        assertThat(request("GET","/api/admin/users/u-bob-scope-b",token,"scope-b",null).status()).isEqualTo(200);
        var request=HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/admin/users/export"))
                .header("Authorization","Bearer "+token).header("X-Cyys-Scope-Id","scope-b")
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{}")) .build();
        var exported=CLIENT.send(request,HttpResponse.BodyHandlers.ofByteArray());
        assertThat(exported.statusCode()).isEqualTo(200);
        try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(exported.body()))) {
            assertThat(workbook.getSheetAt(0).getRow(1).getCell(2).getStringCellValue()).isEqualTo("停用部门乙");
        }
        verify(departments,times(1)).labels("scope-b",List.of("g4-display-b"));
        assertThat(jdbc.queryForObject("select count(*) from sys_data_audit where resource='user' and operation='EXPORT_GENERATED' and scope_id='scope-b'",Integer.class)).isEqualTo(1);
    }
}
