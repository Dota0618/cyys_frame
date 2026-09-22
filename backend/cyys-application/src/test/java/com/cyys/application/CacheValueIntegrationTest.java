package com.cyys.application;

import com.cyys.admin.user.mapper.UserDisplayMapper;
import com.cyys.admin.department.mapper.DepartmentDisplayMapper;
import com.cyys.admin.dictionary.mapper.DictionaryOptionMapper;
import com.cyys.admin.cache.service.BusinessCacheGroups;
import com.cyys.common.cache.*;
import com.cyys.common.satoken.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@SpringBootTest(classes=CyysApplication.class,webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties="cyys.environment=cache-values-acceptance")
class CacheValueIntegrationTest extends HttpTestSupport {
    @MockitoSpyBean UserDisplayMapper users;
    @MockitoSpyBean DictionaryOptionMapper dictionaries;
    @MockitoSpyBean DepartmentDisplayMapper departments;
    @Autowired CacheKeys keys;
    @Autowired StringRedisTemplate redis;
    @Autowired CacheGroupService groups;
    @Autowired TransactionTemplate tx;

    @BeforeEach void cacheFixtures() {
        cleanCaches();
        jdbc.update("update sys_user set status=0 where id='u-bob'");
        jdbc.update("update sys_user_scope set status=0,display_name='停用用户乙' where user_id='u-bob'");
        jdbc.update("insert into param_basic(id,name,system_name,sort) values('g4-cache-types','缓存测试类型','CACHE_TEST_TYPES',0)");
        jdbc.update("insert into param_basic_type(id,basic_id,param_key,param_value,sort) values('g4-cache-item','g4-cache-types','1','中文类型',0)");
        jdbc.update("insert into sys_org(id,scope_id,name,code,leader_id,created_by) values('g4-cache-dept','scope-a','缓存部门','cache-dept','u-bob','u-alice')");
        jdbc.update("insert into sys_org_notice(id,scope_id,org_id,title,type,type_label,content,created_by,created_at,updated_by,updated_at) values('g4-cache-notice','scope-a','g4-cache-dept','缓存通知','1','旧值','正文','u-alice',CURRENT_TIMESTAMP,'u-alice',CURRENT_TIMESTAMP)");
        jdbc.update("insert into sys_org_notice_ref(scope_id,notice_id,user_id) values('scope-a','g4-cache-notice','u-bob')");
        jdbc.update("insert into sys_notice(id,title,type,type_label,content,is_show,created_by,updated_by) values('g4-cache-system','缓存系统通知','missing','不应回退的旧值','正文',1,'u-alice','u-alice')");
        jdbc.update("insert into sys_version(id,release_number,date_update,type,type_label,content,created_by,updated_by) values('g4-cache-release','CACHE',CURRENT_DATE,'missing','不应回退的旧值','正文','u-alice','u-alice')");
        for(String permission:List.of("org-notice:read","org-notice:receipts:read","department:read","system-notice:read","release:read"))
            permission("role-a","g1-cache-"+Integer.toUnsignedString(permission.hashCode()),permission,false);
        clearInvocations(users,dictionaries,departments);
    }
    @AfterEach void removeCacheFixtures() {
        jdbc.update("delete from sys_org_notice_ref where notice_id='g4-cache-notice'");
        jdbc.update("delete from sys_org_notice where id='g4-cache-notice'");
        jdbc.update("delete from sys_notice where id='g4-cache-system'");
        jdbc.update("delete from sys_version where id='g4-cache-release'");
        jdbc.update("delete from param_basic_type where id='g4-cache-item'");
        jdbc.update("delete from param_basic where id='g4-cache-types'");
        cleanCaches();
    }
    void cleanCaches() {
        redis.delete(List.of(keys.platform(BusinessCacheGroups.USER_NAMES.id()),keys.platform(BusinessCacheGroups.DICTIONARY_OPTIONS.id()),
                keys.platform(BusinessCacheGroups.DICTIONARY_LABELS.id()),keys.unit("scope-a",BusinessCacheGroups.DEPARTMENT_NAMES.id())));
    }

    @Test void fiftyRowsResolveDisabledCrossUnitUserAndListOnceWithNoExtraLookupOnHits() throws Exception {
        String token=token("alice");
        var first=request("GET","/api/test/cache-values",token,"scope-a",null);
        assertThat(first.status()).as(first.raw()).isEqualTo(200);
        assertThat(first.body().at("/data/total").asInt()).isEqualTo(50);
        assertThat(first.body().at("/data/records/0/name").asText()).isEqualTo("停用用户乙");
        assertThat(first.body().at("/data/records/0/user/name").asText()).isEqualTo("停用用户乙");
        assertThat(first.body().at("/data/records/0/typeName").asText()).isEqualTo("中文类型");
        assertThat(first.body().at("/data/records/0/types/0/label").asText()).isEqualTo("中文类型");
        var second=request("GET","/api/test/cache-values",token,"scope-a",null);
        assertThat(second.status()).isEqualTo(200);
        verify(users,times(1)).labels(List.of("u-bob"));
        verify(dictionaries,times(1)).options("CACHE_TEST_TYPES");
        assertThat(first.raw()).doesNotContain("password","idCard","id_card","private-id-card");
        assertThat(login("bob",PASSWORD,null).status()).isNotEqualTo(200);
        assertThat(request("GET","/api/test/cache-values",null,"scope-a",null).status()).isEqualTo(401);
    }

    @Test void actualNoticeReadersAndDepartmentsUseAnnotationsAndInvalidationKeepsOtherUsers() throws Exception {
        String token=token("alice");
        var readers=request("GET","/api/admin/org-notices/g4-cache-notice/readers",token,"scope-a",null);
        assertThat(readers.status()).as(readers.raw()).isEqualTo(200);
        assertThat(readers.body().at("/data/records/0/userName").asText()).isEqualTo("停用用户乙");
        var detail=request("GET","/api/admin/org-notices/g4-cache-notice",token,"scope-a",null);
        assertThat(detail.status()).as(detail.raw()).isEqualTo(200);
        assertThat(detail.body().at("/data/notice/departmentName").asText()).isEqualTo("缓存部门");
        request("GET","/api/admin/org-notices/g4-cache-notice",token,"scope-a",null);
        verify(departments,times(1)).labels("scope-a",List.of("g4-cache-dept"));
        String key=keys.platform(BusinessCacheGroups.USER_NAMES.id());
        redis.opsForHash().put(key,"another","another-value");
        tx.executeWithoutResult(status->{
            jdbc.update("update sys_user_scope set display_name='停用后改名' where user_id='u-bob'");
            groups.invalidateEntriesAfterCommit(BusinessCacheGroups.USERS,List.of("u-bob"));
        });
        assertThat(redis.opsForHash().get(key,"another")).isEqualTo("another-value");
        var updated=request("GET","/api/admin/org-notices/g4-cache-notice/readers",token,"scope-a",null);
        assertThat(updated.body().at("/data/records/0/userName").asText()).isEqualTo("停用后改名");
        verify(users,times(2)).labels(List.of("u-bob"));
    }

    @Test void existingDepartmentSystemNoticeAndReleaseModelsUseCacheFieldsWithoutHistoricalFallback() throws Exception {
        String token=token("alice");
        var department=request("GET","/api/admin/departments/g4-cache-dept",token,"scope-a",null);
        assertThat(department.status()).as(department.raw()).isEqualTo(200);
        assertThat(department.body().at("/data/leaderName").asText()).isEqualTo("停用用户乙");
        assertThat(department.body().at("/data/leaderActive").asBoolean()).isFalse();
        request("GET","/api/admin/departments/g4-cache-dept",token,"scope-a",null);
        verify(users,times(1)).labels(List.of("u-bob"));
        for(String path:List.of("/api/admin/platform/system-notices/g4-cache-system", "/api/admin/platform/releases/g4-cache-release")) {
            var detail=request("GET",path,token,"scope-a",null);
            assertThat(detail.status()).as(detail.raw()).isEqualTo(200);
            assertThat(detail.raw()).doesNotContain("不应回退的旧值");
        }
    }
}
