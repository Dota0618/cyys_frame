package com.cyys.application;

import com.cyys.admin.file.controller.FileController;
import com.cyys.admin.file.dto.FileRenameCmd;
import com.cyys.admin.file.service.FileService;
import com.cyys.common.web.ApiException;
import com.cyys.common.web.GlobalExceptionHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 实际文件 Controller 的请求校验和全局响应；不连接数据库。 */
class GlobalExceptionHttpTest {
    private FileService service;
    private MockMvc mvc;
    private LocalValidatorFactoryBean validator;
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void setup() {
        service = mock(FileService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new FileController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
    }

    @AfterEach
    void close() { validator.close(); }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", ".", " .. ", "../secret", "a\\b", "a\nb", "a\u007fb"})
    void invalidNamesAreRejectedAcrossAllFileEntrypoints(String name) throws Exception {
        rejectName(name);
    }

    @Test
    void rejectsOverlongName() throws Exception { rejectName("文".repeat(201)); }

    private void rejectName(String name) throws Exception {
        mvc.perform(put("/api/admin/files/f1").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", name, "version", 0))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("文件名")));
        mvc.perform(post("/api/admin/files/uploads").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", name, "bytes", 1, "hashes", new String[]{"a".repeat(64)}))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(multipart("/api/admin/files").file(new MockMultipartFile("file", name, "text/plain", new byte[]{1})))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("文件名")));
        verifyNoInteractions(service);
    }

    @Test
    void acceptsValidNameAtLengthLimit() throws Exception {
        String name = "文".repeat(200);
        mvc.perform(put("/api/admin/files/f1").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", name, "version", 0))))
                .andExpect(status().isOk());
        verify(service).rename("f1", new FileRenameCmd(name, 0));
    }

    @Test
    void missingNameAndInvalidQueryKeepClientErrorStatus() throws Exception {
        mvc.perform(put("/api/admin/files/f1").contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/files").param("page", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/files").param("page", "invalid")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/files/trash").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void businessFailurePreservesStatusAndMessage() throws Exception {
        when(service.rename(anyString(), any())).thenThrow(new ApiException(409, "文件版本已变化"));
        mvc.perform(put("/api/admin/files/f1").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"报告.txt\",\"version\":0}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.msg").value("文件版本已变化"));
    }

    @Test
    void databaseConflictDoesNotExposeSql() throws Exception {
        when(service.rename(anyString(), any())).thenThrow(new DuplicateKeyException("secret SQL and table name"));
        mvc.perform(put("/api/admin/files/f1").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"报告.txt\",\"version\":0}"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("系统内部错误"));
    }

    @Test
    void requiredFieldsAreRejectedBeforeServicesAcrossChangedControllers() throws Exception {
        var members = mock(com.cyys.admin.user.service.MemberService.class);
        var departments = mock(com.cyys.admin.department.service.DepartmentService.class);
        var menus = mock(com.cyys.admin.menu.service.MenuService.class);
        var roles = mock(com.cyys.admin.role.service.RoleService.class);
        var entrypoints = MockMvcBuilders.standaloneSetup(
                        new com.cyys.admin.user.controller.MemberController(members),
                        new com.cyys.admin.department.controller.DepartmentController(departments),
                        new com.cyys.admin.menu.controller.MenuController(menus),
                        new com.cyys.admin.role.controller.RoleController(roles))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
        for (String path : new String[]{"/api/admin/users", "/api/admin/departments",
                "/api/admin/platform/menus", "/api/admin/roles"}) {
            entrypoints.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        }
        for (String path : new String[]{"/api/admin/users/u1", "/api/admin/users/u1/data-permission",
                "/api/admin/departments", "/api/admin/platform/menus", "/api/admin/roles",
                "/api/admin/roles/accounts/u1/roles"}) {
            entrypoints.perform(put(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        }
        entrypoints.perform(delete("/api/admin/roles/r1").param("version", "-1"))
                .andExpect(status().isBadRequest());
        entrypoints.perform(delete("/api/admin/platform/menus/m1").param("version", "-1"))
                .andExpect(status().isBadRequest());
        for (String path : new String[]{"/api/admin/users", "/api/admin/users/all", "/api/admin/departments",
                "/api/admin/roles", "/api/admin/roles/accounts"}) {
            entrypoints.perform(get(path).param("page", "0")).andExpect(status().isBadRequest());
            entrypoints.perform(get(path).param("size", "201")).andExpect(status().isBadRequest());
        }
        entrypoints.perform(get("/api/admin/departments/leader-options/create").param("search", "x".repeat(101)))
                .andExpect(status().isBadRequest());
        entrypoints.perform(delete("/api/admin/departments").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isBadRequest());
        entrypoints.perform(delete("/api/admin/departments").contentType(MediaType.APPLICATION_JSON).content("[{}]"))
                .andExpect(status().isBadRequest());
        entrypoints.perform(put("/api/admin/users/u1/roles").contentType(MediaType.APPLICATION_JSON).content("[\"\"]"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(members, departments, menus, roles);
    }
}
