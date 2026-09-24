package com.cyys.admin.generator.service;

import com.cyys.admin.generator.dto.*;
import com.cyys.common.web.ApiException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import javax.lang.model.SourceVersion;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

/** Fixed, versioned source templates. User input is data, never a template or output path. */
@Component
public class GeneratorRenderer {
    public static final String VERSION="2026-09-23.1";
    private static final Set<String> RESERVED=Set.of("id","scope_id","org_id","department_id","parent_id","children","version","deleted","created_by","created_at","updated_by","updated_at");
    private static final Set<String> CLASS_NAMES=Set.of("Object","String","Integer","Boolean","BigDecimal","LocalDate","LocalDateTime","List","Map","Page","R","ApiException","System","Override","QueryWrapper",
            "IPage","DataAuditService","SysOrgMapper","DictionaryOption","DictionaryOptionMapper","ScopeContext","RequiredArgsConstructor","Service","Transactional","BaseMapper","ProtectedMapper","Mapper","Select","Update","Param","JsonFormat","DepartmentOption","LinkedHashMap","TableName","DataResourceDefinition","TableId","IdType","TableField","FieldFill","FieldStrategy","Version","TableLogic");
    private static final Set<String> RESERVED_PROPERTIES=Set.of("record","var","yield","permits","sealed","hashCode","toString","clone","finalize","notify","notifyAll","getClass","wait");
    private static final Set<String> FRAMEWORK_FEATURES=Set.of("common","admin","application","modules","business","user","department","role","menu","platform","configuration","dictionary","audit","cache","file","region","release","generator","auth","account","scope","serial");
    private static final Pattern SLOT=Pattern.compile("@@([A-Z_]+)@@");
    private final JsonMapper json;
    public GeneratorRenderer(JsonMapper json) { this.json=json; }

    public void validate(GenerateCodeCommand c) {
        if(c==null || c.table()==null || !c.table().matches("biz_[a-z][a-z0-9_]{0,43}")
                || c.feature()==null || !c.feature().matches("[a-z][a-z0-9]{1,23}") || SourceVersion.isKeyword(c.feature()) || FRAMEWORK_FEATURES.contains(c.feature())
                || c.className()==null || !c.className().matches("[A-Z][A-Za-z0-9]{1,39}") || CLASS_NAMES.contains(c.className())
                || c.title()==null || c.title().isBlank() || c.title().length()>40 || c.layout()==null
                || c.fields()==null || c.fields().isEmpty() || c.fields().size()>30) throw invalid("生成配置无效");
        Set<String> columns=new HashSet<>(), properties=new HashSet<>();
        int textCapacity=0;
        for(var f:c.fields()) {
            if(f==null || f.column()==null || !f.column().matches("[a-z][a-z0-9]*(?:_[a-z0-9]+)*") || f.column().length()>40
                    || RESERVED.contains(f.column()) || !columns.add(f.column()) || f.type()==null
                    || f.label()==null || f.label().isBlank() || f.label().length()>40) throw invalid("字段名、类型或名称无效，不能使用框架保留字段");
            String property=property(f.column());
            if(SourceVersion.isKeyword(property) || RESERVED_PROPERTIES.contains(property) || !properties.add(property)) throw invalid("字段转换后的 Java 属性名无效或重复");
            if(f.searchable() && Set.of("page","size","query","paging").contains(property)) throw invalid("查询字段名称与分页参数冲突");
            if((f.type()==GeneratorFieldType.TEXT || f.type()==GeneratorFieldType.TEXTAREA) && (f.length()==null || f.length()<1 || f.length()>2000)) throw invalid("文本长度须为 1–2000");
            if(f.type()==GeneratorFieldType.TEXT || f.type()==GeneratorFieldType.TEXTAREA) textCapacity+=f.length();
            if(f.type()==GeneratorFieldType.DICTIONARY) textCapacity+=20;
            if(f.searchable() && f.type()!=GeneratorFieldType.TEXT && f.type()!=GeneratorFieldType.DICTIONARY) throw invalid("查询字段仅支持短文本和字典");
            if(f.type()==GeneratorFieldType.DICTIONARY && (f.dictionary()==null || !f.dictionary().matches("[A-Z][A-Z0-9_]{0,63}"))) throw invalid("字典字段须指定固定内部名称");
        }
        if(textCapacity>12000) throw invalid("文本字段声明长度合计不能超过 12000，请拆分业务数据以避免 MySQL 行长度超限");
        if(c.fields().getFirst().type()!=GeneratorFieldType.TEXT || !c.fields().getFirst().required()) throw invalid("第一个字段须为必填短文本，作为记录和树节点名称");
    }

    public GeneratorPreview preview(GenerateCodeCommand c) {
        validate(c);
        Map<String,String> v=new HashMap<>();
        v.put("PACKAGE","com.cyys."+c.feature()); v.put("FEATURE",c.feature()); v.put("CLASS",c.className());
        v.put("TABLE",c.table()); v.put("TITLE",quote(c.title())); v.put("TITLE_HTML","{"+quote(c.title())+"}");
        v.put("LABEL_PROPERTY",property(c.fields().getFirst().column()));
        boolean tree=c.layout()==GeneratorLayout.TREE;
        v.put("PARENT_FIELD",tree?"@TableField(updateStrategy=FieldStrategy.NEVER) private String parentId;\n    public String getParentId() { return parentId; }\n    public void setParentId(String value) { parentId=value; }":"");
        v.put("PARENT_CREATE",tree?"@Size(max=32) String parentId,":"");
        v.put("PARENT_VIEW",tree?"String parentId,":"");
        v.put("PARENT_VIEW_VALUE",tree?"row.getParentId(),":"");
        v.put("PARENT_ASSIGN",tree?"row.setParentId(command.parentId());\n        if(command.parentId()!=null) { var parent=require(command.parentId(),true); if(!parent.getOrgId().equals(command.departmentId())) throw new ApiException(409,\"父节点必须属于同一部门\"); }":"");
        v.put("PARENT_DELETE",tree?"if(mapper.hasChildren(id,ScopeContext.get().requireScopeId())) throw new ApiException(409,\"存在下级，不能删除\");":"");
        v.put("PARENT_SQL",tree?"parent_id varchar(32) COMMENT '父记录编号',\n    ":"");
        v.put("PARENT_INDEX",tree?"KEY idx_"+c.feature()+"_parent (scope_id,parent_id,deleted),":"");
        v.put("IMMUTABLE",tree?"\"org_id\",\"parent_id\"":"\"org_id\"");
        // Child existence is a dependency guard: checking all same-unit children cannot expose their content.
        v.put("CHILD_MAPPER",tree?"@Select(\"select exists(select 1 from "+c.table()+" where parent_id=#{id} and scope_id=#{scope} and deleted=0)\")\n    boolean hasChildren(@Param(\"id\") String id,@Param(\"scope\") String scope);":"");
        v.put("TREE_IMPORT",tree?"import "+v.get("PACKAGE")+".mapper."+c.className()+"DependencyMapper;":"");
        v.put("TREE_DEPENDENCY",tree?"private final "+c.className()+"DependencyMapper dependencies;":"");
        v.put("PARENT_DELETE",v.get("PARENT_DELETE").replace("mapper.hasChildren","dependencies.hasChildren"));
        var model=new StringBuilder(); var create=new ArrayList<String>(); var view=new ArrayList<String>(); var values=new ArrayList<String>();
        var assign=new StringBuilder(); var dict=new StringBuilder(); var sql=new ArrayList<String>();
        var ts=new ArrayList<String>(); var form=new StringBuilder(); var table=new StringBuilder();
        var searchArgs=new ArrayList<String>(); var searchPass=new ArrayList<String>(); var searchQuery=new StringBuilder();
        var searchTs=new StringBuilder(); var searchService=new StringBuilder();
        for(var f:c.fields()) {
            String p=property(f.column()), cap=upper(p), type=javaType(f);
            model.append("@TableField(value=\"`").append(f.column()).append("`\",updateStrategy=FieldStrategy.ALWAYS) private ").append(type).append(' ').append(p).append(";\n    public ").append(type).append(" get").append(cap).append("() { return ").append(p).append("; }\n    public void set").append(cap).append('(').append(type).append(" value) { ").append(p).append("=value; }\n    ");
            create.add(validation(f)+type+" "+p); view.add((f.type()==GeneratorFieldType.DECIMAL?"@JsonFormat(shape=JsonFormat.Shape.STRING) ":"")+type+" "+p); values.add("row.get"+cap+"()");
            assign.append("row.set").append(cap).append("(command.").append(p).append("());\n        ");
            if(f.type()==GeneratorFieldType.DICTIONARY) dict.append("if(command.").append(p).append("()!=null && !java.util.Objects.equals(command.").append(p).append("(),previous==null?null:previous.get").append(cap).append("()) && dictionaries.eligible(").append(quote(f.dictionary())).append(",command.").append(p).append("())==null) throw new ApiException(409,\"字典选项已失效\");\n        ");
            sql.add("    `"+f.column()+"` "+sqlType(f)+(f.required()?" NOT NULL":"")+" COMMENT "+sqlComment(f.label()));
            ts.add(p+(f.required()?"":"?")+": "+tsType(f)+(f.required()?"":" | null")+";");
            form.append(formField(f));
            if(f.inList() || f.searchable()) table.append("{ title: ").append(quote(f.label())).append(", dataIndex: ").append(quote(p)).append(", hideInTable: ").append(!f.inList()).append(", search: ").append(f.searchable()).append(f.type()==GeneratorFieldType.BOOLEAN?", renderText: (value: boolean) => value ? '是' : '否'":"").append(" },\n    ");
            if(f.searchable()) {
                searchArgs.add("@RequestParam(required=false) @Size(max="+(f.type()==GeneratorFieldType.DICTIONARY?20:f.length())+") String "+p);
                searchPass.add(p); searchQuery.append("query.").append(f.type()==GeneratorFieldType.DICTIONARY?"eq":"like").append('(').append(p).append("!=null && !").append(p).append(".isBlank(),\"`").append(f.column()).append("`\",").append(p).append(");\n        ");
                searchTs.append("if (params.").append(p).append(" !== undefined && params.").append(p).append(" !== '') query.set(").append(quote(p)).append(", String(params.").append(p).append("));\n  ");
                searchService.append(",String ").append(p);
            }
        }
        v.put("MODEL_FIELDS",model.toString()); v.put("COMMAND_FIELDS",String.join(",\n        ",create));
        v.put("VIEW_FIELDS",String.join(", ",view)); v.put("VIEW_VALUES",String.join(", ",values));
        v.put("ASSIGN",assign.toString()); v.put("DICT_CHECK",dict.toString()); v.put("SQL_FIELDS",String.join(",\n",sql));
        v.put("SEARCH_ARGS",searchArgs.isEmpty()?"":","+String.join(",",searchArgs));
        v.put("SEARCH_PASS",searchPass.isEmpty()?"":","+String.join(",",searchPass));
        v.put("SEARCH_SERVICE",searchService.toString()); v.put("SEARCH_QUERY",searchQuery.toString());
        v.put("TS_FIELDS",String.join("\n  ",ts)); v.put("FORM_FIELDS",form.toString()); v.put("COLUMNS",table.toString());
        v.put("SEARCH_TS",searchTs.toString()); v.put("TREE",Boolean.toString(tree));
        v.put("FORM_COMMAND",c.fields().stream().map(f->property(f.column())+": "+(!f.required() && f.type()==GeneratorFieldType.DECIMAL?"values."+property(f.column())+" === '' ? null : ":"")+"values."+property(f.column())+(f.required()?"":" ?? null")+",").collect(java.util.stream.Collectors.joining("\n          ")));
        v.put("PARENT_TS",tree?"parentId?: string | null;":"");
        v.put("PARENT_FORM",tree?"<ProFormSelect name=\"parentId\" label=\"父节点\" allowClear options={options.parents.map(row => ({ value: row.id, label: row."+v.get("LABEL_PROPERTY")+" }))} />":"");
        v.put("PARENT_COMMAND",tree?"parentId: values.parentId ?? null,":"");
        v.put("DICT_OPTIONS",dictionaryOptions(c));
        var result=new ArrayList<GeneratedSource>();
        String javaRoot="backend/cyys-"+c.feature()+"/src/main/java/com/cyys/"+c.feature()+"/";
        for(String kind:List.of("Model","Create","Update","View","Options","Mapper","Service","Controller")) {
            String path=switch(kind) {
                case "Model" -> "model/"+c.className(); case "Create","Update" -> "dto/"+kind+c.className()+"Command";
                case "View","Options" -> "dto/"+c.className()+kind; default -> kind.toLowerCase(Locale.ROOT)+"/"+c.className()+kind;
            };
            result.add(new GeneratedSource(javaRoot+path+".java",template(kind+".java.tpl",v)));
        }
        if(tree) result.add(new GeneratedSource(javaRoot+"mapper/"+c.className()+"DependencyMapper.java",template("DependencyMapper.java.tpl",v)));
        result.add(new GeneratedSource("admin-ui/src/pages/business/"+c.feature()+"/index.tsx",template("Page.tsx.tpl",v)));
        result.add(new GeneratedSource("admin-ui/src/services/business/"+c.feature()+".ts",template("Requests.ts.tpl",v)));
        if(!c.existingTable()) result.add(new GeneratedSource("migrations/business/PENDING__"+c.feature()+".sql",template("Table.sql.tpl",v)));
        result.add(new GeneratedSource("permissions.sql.review",permissions(c)));
        result.add(new GeneratedSource("generator-config.json",json.writeValueAsString(c)));
        result.add(new GeneratedSource("INTEGRATION.md",template("Integration.md.tpl",v)));
        try {
            String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.writeValueAsBytes(result)));
            return new GeneratorPreview(VERSION,digest,List.copyOf(result));
        } catch(java.security.NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }

    public byte[] zip(GenerateCodeCommand c) {
        var preview=preview(c);
        try(var bytes=new ByteArrayOutputStream();var zip=new ZipOutputStream(bytes,StandardCharsets.UTF_8)) {
            for(var file:preview.files()) {
                var entry=new ZipEntry(file.path()); entry.setTime(0); zip.putNextEntry(entry);
                zip.write(file.content().getBytes(StandardCharsets.UTF_8));zip.closeEntry();
            }
            zip.finish(); if(bytes.size()>2_000_000) throw invalid("生成结果过大");return bytes.toByteArray();
        } catch(IOException failure) { throw new IllegalStateException("生成包失败",failure); }
    }

    private String template(String name,Map<String,String> values) {
        try(var stream=getClass().getResourceAsStream("/generator/"+name)) {
            if(stream==null) throw new IllegalStateException("Missing bundled template: "+name);
            String content=new String(stream.readAllBytes(),StandardCharsets.UTF_8);
            var matcher=SLOT.matcher(content);var result=new StringBuilder();
            while(matcher.find()) { String value=values.get(matcher.group(1));if(value==null) throw new IllegalStateException("Missing template slot");matcher.appendReplacement(result,Matcher.quoteReplacement(value)); }
            matcher.appendTail(result);return result.toString();
        } catch(IOException failure) { throw new IllegalStateException("Cannot read bundled template",failure); }
    }
    private String dictionaryOptions(GenerateCodeCommand c) {
        return c.fields().stream().filter(f->f.type()==GeneratorFieldType.DICTIONARY).map(f->"result.put("+quote(property(f.column()))+",dictionaries.options("+quote(f.dictionary())+"));").collect(java.util.stream.Collectors.joining("\n        "));
    }
    private String permissions(GenerateCodeCommand c) {
        var result=new StringBuilder("-- Review before applying. Register the local route, select a parent directory, then grant menu and button nodes explicitly.\n");
        String menuCode="nav:menu:"+c.feature();
        result.append("INSERT INTO sys_menu(id,code,name,parent_id,type,path,platform_only,created_by) VALUES(REPLACE(UUID(),'-',''),'")
                .append(menuCode).append("',").append(sqlText(c.title())).append(",'0',2,'/business/")
                .append(c.feature()).append("',0,'generator');\n");
        var labels=Map.of("read","查看","create","新增","update","修改","delete","删除");
        for(String action:List.of("read","create","update","delete")) {
            String code=c.feature()+":"+action;
            result.append("INSERT INTO sys_menu(id,code,name,parent_id,type,platform_only,created_by) SELECT REPLACE(UUID(),'-',''),'")
                    .append(code).append("',").append(sqlText(labels.get(action)+c.title()))
                    .append(",id,3,0,'generator' FROM sys_menu WHERE code='").append(menuCode).append("';\n");
        }
        return result.toString();
    }
    private static String sqlText(String value) {
        return "CONVERT(0x"+java.util.HexFormat.of().formatHex(value.getBytes(StandardCharsets.UTF_8))+" USING utf8mb4)";
    }
    private static String sqlComment(String value) {
        return "'"+value.replace("\\", "\\\\").replace("'", "\\'")+"'";
    }
    private String formField(GeneratorField f) {
        String p=property(f.column());
        String rules="rules={["+(f.required()?"{ required: true, message: '请填写此字段' }":"")+"]}";
        String common="name="+quote(p)+" label={"+quote(f.label())+"} "+rules;
        return switch(f.type()) {
            case TEXT -> "<ProFormText "+common+" fieldProps={{ maxLength: "+f.length()+" }} />\n      ";
            case TEXTAREA -> "<ProFormTextArea formItemProps={verticalFormItemProps} "+common+" fieldProps={{ maxLength: "+f.length()+", rows: 4 }} />\n      ";
            case INTEGER -> "<ProFormDigit "+common+" fieldProps={{ precision: 0, min: -2147483648, max: 2147483647 }} />\n      ";
            case DECIMAL -> "<ProFormText name="+quote(p)+" label={"+quote(f.label())+"} rules={[{ required: "+f.required()+" }, { pattern: /^-?\\d{1,16}(\\.\\d{1,2})?$/, message: '最多 16 位整数、2 位小数' }]} />\n      ";
            case BOOLEAN -> "<ProFormSwitch name="+quote(p)+" label={"+quote(f.label())+"} initialValue={false} />\n      ";
            case DATE -> "<ProFormDatePicker "+common+" transform={(value: string | null) => ({ "+p+": value || null })} />\n      ";
            case DATETIME -> "<ProFormDateTimePicker "+common+" transform={(value: string | null) => ({ "+p+": value ? value.replace(' ', 'T') : null })} />\n      ";
            case DICTIONARY -> "<ProForm.Item "+common+"><DictionarySelect ariaLabel={"+quote(f.label())+"} options={options.dictionaries."+p+" || []} historical={editing?."+p+" ? { value: editing."+p+", label: editing."+p+", disabled: !(options.dictionaries."+p+" || []).some(item => item.value === editing."+p+") } : undefined} /></ProForm.Item>\n      ";
        };
    }
    private static String validation(GeneratorField f) {
        String required=f.required()?(Set.of(GeneratorFieldType.TEXT,GeneratorFieldType.TEXTAREA,GeneratorFieldType.DICTIONARY).contains(f.type())?"@NotBlank ":"@NotNull "):"";
        return required+switch(f.type()) {
            case TEXT,TEXTAREA -> "@Size(max="+f.length()+") "; case DICTIONARY -> "@Size(max=20) ";
            case DECIMAL -> "@Digits(integer=16,fraction=2) ";
            case DATE -> "@JsonFormat(shape=JsonFormat.Shape.STRING,pattern=\"uuuu-MM-dd\",lenient=OptBoolean.FALSE) ";
            case DATETIME -> "@JsonFormat(shape=JsonFormat.Shape.STRING,pattern=\"uuuu-MM-dd'T'HH:mm:ss\",lenient=OptBoolean.FALSE) ";
            default -> "";
        };
    }
    public static String sqlType(GeneratorField f) { return switch(f.type()) {
        case TEXT,TEXTAREA -> "varchar("+f.length()+")";case DICTIONARY -> "varchar(20)";case INTEGER -> "int";
        case DECIMAL -> "decimal(18,2)";case BOOLEAN -> "tinyint(1)";case DATE -> "date";case DATETIME -> "datetime";
    }; }
    private static String javaType(GeneratorField f) { return switch(f.type()) {case INTEGER->"Integer";case DECIMAL->"BigDecimal";case BOOLEAN->"Boolean";case DATE->"LocalDate";case DATETIME->"LocalDateTime";default->"String";}; }
    private static String tsType(GeneratorField f) {return f.type()==GeneratorFieldType.INTEGER?"number":f.type()==GeneratorFieldType.BOOLEAN?"boolean":"string";}
    private String quote(String s) {return json.writeValueAsString(s);}
    private static String upper(String s) {return Character.toUpperCase(s.charAt(0))+s.substring(1);}
    public static String property(String name) {String[] parts=name.split("_");StringBuilder result=new StringBuilder(parts[0]);for(int i=1;i<parts.length;i++) result.append(upper(parts[i]));return result.toString();}
    private static ApiException invalid(String message) {return new ApiException(400,message);}
}
