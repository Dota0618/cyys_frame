package com.cyys.admin.generator.service;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.PlatformSecurityService;
import com.cyys.admin.generator.dto.*;
import com.cyys.admin.generator.mapper.GeneratorMetadataMapper;
import com.cyys.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GeneratorService {
    private final PlatformSecurityService security;
    private final GeneratorMetadataMapper metadata;
    private final GeneratorRenderer renderer;
    private final DataAuditService audit;

    @Transactional(readOnly=true)
    public List<GeneratorTable> tables() {
        security.authorize("generator:read");
        var rows=metadata.tables();
        if(rows.size()>200) throw new ApiException(422,"业务表超过 200 张，请按项目拆分生成任务");
        return rows;
    }

    @Transactional(readOnly=true)
    public List<GeneratorColumn> columns(String table) {
        security.authorize("generator:read");
        if(!table.matches("biz_[a-z][a-z0-9_]{0,43}")) throw new ApiException(400,"请选择业务表");
        var columns=metadata.columns(table);
        if(columns.isEmpty()) throw new ApiException(404,"业务表不存在");
        if(columns.size()>100) throw new ApiException(422,"业务表字段过多");
        return columns;
    }

    @Transactional(readOnly=true)
    public GeneratorPreview preview(GenerateCodeCommand command) {
        security.authorize("generator:read");
        checkSchema(command);
        var result=renderer.preview(command);
        security.authorize("generator:read");
        return result;
    }

    @Transactional
    public byte[] download(GenerateCodeCommand command) {
        security.authorize("generator:generate");
        checkSchema(command);
        byte[] result=renderer.zip(command);
        security.authorize("generator:generate");
        audit.platformSuccess("generator",command.feature(),"GENERATE");
        return result;
    }

    private void checkSchema(GenerateCodeCommand command) {
        renderer.validate(command);
        var columns=metadata.columns(command.table());
        if(!command.existingTable()) {
            if(!columns.isEmpty()) throw new ApiException(409,"表已存在，请使用已有表模式并核对全部字段");
            return;
        }
        var expected=new LinkedHashMap<String,String>();
        expected.put("id","varchar(32)"); expected.put("scope_id","varchar(32)"); expected.put("org_id","varchar(32)");
        expected.put("version","int"); expected.put("deleted","tinyint");
        expected.put("created_by","varchar(32)"); expected.put("created_at","datetime");
        expected.put("updated_by","varchar(32)"); expected.put("updated_at","datetime");
        if(command.layout()==GeneratorLayout.TREE) expected.put("parent_id","varchar(32)");
        command.fields().forEach(f->expected.put(f.column(),GeneratorRenderer.sqlType(f)));
        if(columns.size()!=expected.size()) throw new ApiException(409,"已有表须完整匹配生成配置及单位/部门/审计/版本字段，不能遗漏字段");
        var optional=new HashSet<String>();
        if(command.layout()==GeneratorLayout.TREE) optional.add("parent_id");
        command.fields().stream().filter(f->!f.required()).forEach(f->optional.add(f.column()));
        for(var column:columns) {
            String wanted=expected.remove(column.name());
            String actual=column.columnType().toLowerCase(Locale.ROOT).replaceAll("^(tinyint|int)\\(\\d+\\)$","$1");
            String normalized=wanted==null?null:wanted.replace("tinyint(1)","tinyint");
            if(!Objects.equals(actual,normalized) || column.nullable()!=optional.contains(column.name())
                    || !Set.of("","DEFAULT_GENERATED").contains(column.extra()) || (column.name().equals("id")?!"PRI".equals(column.keyType()):"PRI".equals(column.keyType())))
                throw new ApiException(409,"已有表字段不兼容："+column.name()+"，请先人工核对结构");
        }
        if(!expected.isEmpty()) throw new ApiException(409,"已有表缺少必要字段");
    }
}
