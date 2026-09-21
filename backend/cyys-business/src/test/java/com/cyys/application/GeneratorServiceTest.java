package com.cyys.application;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.cyys.admin.auth.mapper.PlatformSecurityMapper;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.generator.dto.*;
import com.cyys.admin.generator.mapper.GeneratorMetadataMapper;
import com.cyys.admin.generator.service.*;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeneratorServiceTest {
    private final DataAuthorizationMapper authorization=mock(DataAuthorizationMapper.class);
    private final GeneratorMetadataMapper metadata=mock(GeneratorMetadataMapper.class);
    private final DataAuditService audit=mock(DataAuditService.class);
    private final GeneratorService service=new GeneratorService(
            new PlatformSecurityService(authorization,mock(PlatformSecurityMapper.class)),metadata,
            new GeneratorRenderer(JsonMapper.builder().build()),audit);

    private void identity() {
        ScopeContext.set(new RequestIdentity("actor","unit-a",List.of(),List.of("generator:read","generator:generate")));
    }
    private final Set<String> granted = new HashSet<>();
    private void grant(String permission) {
        granted.add(permission);
        when(authorization.permissions("actor")).thenAnswer(call -> List.copyOf(granted));
        when(authorization.hasPermission("actor", "platform:scope:access")).thenReturn(true);
        when(authorization.hasPermission("actor", permission)).thenReturn(true);
    }
    @AfterEach void clear() { ScopeContext.clear(); }

    @Test void missingOperationGrantCannotReadMetadataOrGenerate() {
        identity();
        assertThatThrownBy(service::tables).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->service.download(GeneratorRendererTest.sample(GeneratorLayout.LIST))).isInstanceOf(ApiException.class);
        verifyNoInteractions(metadata,audit);
    }

    @Test void previewGrantDoesNotGrantDownloadAndRevocationPreventsDelivery() {
        identity();grant("generator:read");
        var command=GeneratorRendererTest.sample(GeneratorLayout.LIST);
        when(metadata.columns(command.table())).thenReturn(List.of());
        assertThat(service.preview(command).files()).isNotEmpty();
        assertThatThrownBy(()->service.download(command)).isInstanceOf(ApiException.class);
        grant("generator:generate");
        when(authorization.permissions("actor")).thenReturn(List.of("generator:generate"), List.of());
        assertThatThrownBy(()->service.download(command)).isInstanceOf(ApiException.class);
        verifyNoInteractions(audit);
    }

    @Test void downloadAuditsSuccessAndRejectsExistingTableConflict() {
        identity();grant("generator:generate");
        var command=GeneratorRendererTest.sample(GeneratorLayout.LIST);
        when(metadata.columns(command.table())).thenReturn(List.of());
        assertThat(service.download(command)).startsWith((byte)'P',(byte)'K');
        verify(audit).platformSuccess("generator","sample","GENERATE");
        clearInvocations(audit);
        when(metadata.columns(command.table())).thenReturn(List.of(column("id","varchar(32)",false,"PRI","")));
        assertThatThrownBy(()->service.download(command)).isInstanceOf(ApiException.class).hasMessageContaining("表已存在");
        verifyNoInteractions(audit);
    }

    @Test void existingTableRequiresFullCompatibleSchemaAndEmitsNoDdl() {
        identity();grant("generator:read");
        var source=GeneratorRendererTest.sample(GeneratorLayout.TREE);
        var command=new GenerateCodeCommand(source.table(),source.feature(),source.className(),source.title(),source.layout(),true,source.fields());
        var columns=new ArrayList<GeneratorColumn>();
        for(var name:List.of("id","scope_id","org_id","created_by","updated_by")) columns.add(column(name,"varchar(32)",false,name.equals("id")?"PRI":"",""));
        columns.add(column("parent_id","varchar(32)",true,"",""));
        columns.add(column("version","int",false,"",""));
        columns.add(column("deleted","tinyint",false,"",""));
        for(var name:List.of("created_at","updated_at")) columns.add(column(name,"datetime",false,"","DEFAULT_GENERATED"));
        for(var field:source.fields()) columns.add(column(field.column(),GeneratorRenderer.sqlType(field),!field.required(),"",""));
        when(metadata.columns(command.table())).thenReturn(columns);
        assertThat(service.preview(command).files()).noneMatch(f->f.path().startsWith("migrations/"));
        columns.set(0,column("id","varchar(32)",true,"PRI",""));
        assertThatThrownBy(()->service.preview(command)).isInstanceOf(ApiException.class).hasMessageContaining("id");
        columns.add(column("unknown","int",true,"",""));
        assertThatThrownBy(()->service.preview(command)).isInstanceOf(ApiException.class).hasMessageContaining("不能遗漏字段");
    }

    private static GeneratorColumn column(String name,String type,boolean nullable,String key,String extra) {
        return new GeneratorColumn(name,type.replaceAll("\\(.*", ""),type,null,null,nullable,key,extra,"");
    }
}
