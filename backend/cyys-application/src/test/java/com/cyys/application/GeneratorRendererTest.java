package com.cyys.application;

import com.cyys.admin.generator.dto.*;
import com.cyys.admin.generator.service.GeneratorRenderer;
import com.cyys.common.web.ApiException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import javax.tools.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;

class GeneratorRendererTest {
    private final GeneratorRenderer renderer=new GeneratorRenderer(JsonMapper.builder().build());

    static GenerateCodeCommand sample(GeneratorLayout layout) {
        return new GenerateCodeCommand("biz_generator_sample","sample","SampleRecord","示例\"内容</script>",layout,false,List.of(
                new GeneratorField("name","名称",GeneratorFieldType.TEXT,true,true,true,100,null),
                new GeneratorField("notes","备注",GeneratorFieldType.TEXTAREA,false,false,false,500,null),
                new GeneratorField("quantity","数量",GeneratorFieldType.INTEGER,false,true,false,null,null),
                new GeneratorField("amount","金额",GeneratorFieldType.DECIMAL,false,true,false,null,null),
                new GeneratorField("enabled","启用",GeneratorFieldType.BOOLEAN,true,true,false,null,null),
                new GeneratorField("due_date","到期日期",GeneratorFieldType.DATE,false,true,false,null,null),
                new GeneratorField("occurred_at","发生时间",GeneratorFieldType.DATETIME,false,true,false,null,null),
                new GeneratorField("kind","类别",GeneratorFieldType.DICTIONARY,false,true,true,null,"NOTICE_TYPE")));
    }

    @Test void ordinaryAndTreeSourcesActuallyCompileAndZipMatchesPreview() throws Exception {
        for(var layout:GeneratorLayout.values()) {
            var command=sample(layout);var preview=renderer.preview(command);
            assertThat(renderer.preview(command)).isEqualTo(preview);
            var root=Path.of("target/generator-verification/"+layout.name().toLowerCase(Locale.ROOT)).toAbsolutePath();
            var files=new ArrayList<File>();
            for(var source:preview.files()) {
                if (source.path().endsWith(".java")) {
                    assertThat(source.path()).startsWith("backend/cyys-sample/src/main/java/com/cyys/sample/");
                    assertThat(source.content()).contains("package com.cyys.sample.").doesNotContain("com.cyys.modules.");
                }
                Path path=root.resolve(source.path()).normalize();
                assertThat(path.startsWith(root)).isTrue();
                Files.createDirectories(path.getParent());Files.writeString(path,source.content());
                if(path.toString().endsWith(".java")) files.add(path.toFile());
            }
            var classes=root.resolve("compiled");Files.createDirectories(classes);
            var diagnostics=new DiagnosticCollector<JavaFileObject>();var compiler=ToolProvider.getSystemJavaCompiler();
            try(var manager=compiler.getStandardFileManager(diagnostics,null,StandardCharsets.UTF_8)) {
                boolean success=compiler.getTask(null,manager,diagnostics,List.of("--release","25","-parameters","-encoding","UTF-8",
                                "-classpath",System.getProperty("java.class.path"),"-processor","lombok.launch.AnnotationProcessorHider$AnnotationProcessor","-d",classes.toString()),
                        null,manager.getJavaFileObjectsFromFiles(files)).call();
                assertThat(success).as("%s compile: %s",layout,diagnostics.getDiagnostics()).isTrue();
            }
            var zipped=new LinkedHashMap<String,String>();
            try(var zip=new ZipInputStream(new ByteArrayInputStream(renderer.zip(command)),StandardCharsets.UTF_8)) {
                ZipEntry entry;while((entry=zip.getNextEntry())!=null) zipped.put(entry.getName(),new String(zip.readAllBytes(),StandardCharsets.UTF_8));
            }
            assertThat(zipped).hasSize(preview.files().size());
            for(var file:preview.files()) assertThat(zipped.get(file.path())).isEqualTo(file.content());
            assertThat(zipped.get("generator-config.json")).doesNotContain("password","token");
            assertThat(zipped.get("permissions.sql.review")).contains("'nav:menu:sample'", "'/business/sample'", "parent_id", "CONVERT(0x")
                    .contains("FROM sys_menu WHERE code='nav:menu:sample'").doesNotContain("示例\"内容</script>");
        }
    }

    @Test void reservedOwnershipFieldsPathsAndInvalidDictionaryDefinitionsAreRejected() {
        var good=sample(GeneratorLayout.LIST);
        for (String feature : List.of("common", "admin", "application", "modules", "business")) {
            assertThatThrownBy(() -> renderer.preview(new GenerateCodeCommand(good.table(), feature, good.className(),
                    good.title(), good.layout(), false, good.fields()))).isInstanceOf(ApiException.class);
        }
        for(String table:List.of("sys_user","../escape","biz_x;drop_table","biz_x/../../")) {
            assertThatThrownBy(()->renderer.preview(new GenerateCodeCommand(table,good.feature(),good.className(),good.title(),good.layout(),false,good.fields())))
                    .isInstanceOf(ApiException.class);
        }
        for(String column:List.of("scope_id","created_by","department_id","children","class","record","hash_code","to_string","x\";")) {
            assertThatThrownBy(()->renderer.preview(new GenerateCodeCommand(good.table(),good.feature(),good.className(),good.title(),good.layout(),false,
                    List.of(new GeneratorField(column,"字段",GeneratorFieldType.TEXT,true,true,false,100,null)))))
                    .isInstanceOf(ApiException.class);
        }
        assertThatThrownBy(()->renderer.preview(new GenerateCodeCommand(good.table(),good.feature(),good.className(),good.title(),good.layout(),false,
                List.of(good.fields().getFirst(),good.fields().getFirst())))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->renderer.preview(new GenerateCodeCommand(good.table(),good.feature(),good.className(),good.title(),good.layout(),false,
                List.of(good.fields().getFirst(),new GeneratorField("kind","类别",GeneratorFieldType.DICTIONARY,false,true,false,null,"bad'name"))))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->renderer.preview(new GenerateCodeCommand(good.table(),good.feature(),"DataAuditService",good.title(),good.layout(),false,good.fields()))).isInstanceOf(ApiException.class);
        var wide=new ArrayList<GeneratorField>();
        for(int i=0;i<7;i++) wide.add(new GeneratorField("field"+i,"字段",GeneratorFieldType.TEXT,true,true,false,2000,null));
        assertThatThrownBy(()->renderer.preview(new GenerateCodeCommand(good.table(),good.feature(),good.className(),good.title(),good.layout(),false,wide))).isInstanceOf(ApiException.class);
    }
}
