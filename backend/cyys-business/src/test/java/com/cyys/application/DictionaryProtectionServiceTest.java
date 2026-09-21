package com.cyys.application;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.dictionary.dto.*;
import com.cyys.admin.dictionary.mapper.DictionaryMapper;
import com.cyys.admin.dictionary.model.ParamBasic;
import com.cyys.admin.dictionary.service.DictionaryService;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DictionaryProtectionServiceTest {
    private final DictionaryMapper mapper = mock(DictionaryMapper.class);
    private final DataAuditService audit = mock(DataAuditService.class);
    private final DictionaryService service = new DictionaryService(mapper, audit, mock(com.cyys.common.cache.CacheGroupService.class));

    @BeforeEach void identity() {
        ScopeContext.set(new RequestIdentity("actor", null, List.of("SUPER_ADMIN"), List.of()));
    }
    @AfterEach void clear() { ScopeContext.clear(); }

    private ParamBasic dictionary(boolean builtin) {
        var row = new ParamBasic();
        row.setId("1"); row.setName("是否状态"); row.setSystemName("YES_NO");
        row.setSort(0); row.setSystemBuiltin(builtin);
        return row;
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "delete", "createItem", "updateItem", "deleteItem"})
    void systemDictionaryRejectsEveryWriteEvenWithPlatformAuthorization(String operation) {
        var row = dictionary(true);
        when(mapper.lock("1")).thenReturn(row);
        assertThatThrownBy(() -> {
            switch (operation) {
                case "update" -> service.update("1", new UpdateDictionaryCommand(0, "修改", 0));
                case "delete" -> service.delete("1", 0);
                case "createItem" -> service.createItem("1", new CreateDictionaryItemCommand("x", "新增", null, 0));
                case "updateItem" -> service.updateItem("1", "item", new UpdateDictionaryItemCommand(0, "修改", null, 0));
                case "deleteItem" -> service.deleteItem("1", "item", 0);
                default -> throw new AssertionError(operation);
            }
        }).isInstanceOfSatisfying(ApiException.class, error -> {
            assertThat(error.getStatus()).isEqualTo(403);
            assertThat(error.getMessage()).contains("系统字典", "只读");
        });
        verify(mapper).lock("1");
        verifyNoMoreInteractions(mapper);
        verifyNoInteractions(audit);
        assertThat(row.getName()).isEqualTo("是否状态");
    }

    @Test void systemDictionaryRemainsReadable() {
        when(mapper.detail("1")).thenReturn(dictionary(true));
        assertThat(service.detail("1").systemBuiltin()).isTrue();
    }

    @Test void businessDictionaryCanBeCreatedAndUpdated() {
        when(mapper.create(any(), eq("actor"))).thenReturn(1);
        var created = service.create(new CreateDictionaryCommand("80", "业务分类", "BUSINESS", null));
        assertThat(created.systemBuiltin()).isFalse();
        when(mapper.lock("1")).thenReturn(dictionary(false));
        when(mapper.update(any(), eq("actor"))).thenReturn(1);
        var updated = service.update("1", new UpdateDictionaryCommand(0, "新分类", 2));
        assertThat(updated.name()).isEqualTo("新分类");
        assertThat(updated.version()).isEqualTo(1);
        assertThat(updated.systemBuiltin()).isFalse();
        verify(audit).platformSuccess("dictionary", "1", "UPDATE");
    }
}
