package com.cyys.application;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;

/** 对已批准的目录与依赖边界做轻量检查，不引入额外架构测试框架。 */
class ArchitectureRulesTest {
    @Test
    void featureLayersAndImportsFollowApprovedSchemeA() throws Exception {
        Path backend = Path.of("..").toAbsolutePath().normalize();
        for (String module : List.of("cyys-common", "cyys-admin", "cyys-business")) {
            Path sourceRoot = backend.resolve(module + "/src/main/java");
            try (var paths = Files.walk(sourceRoot)) {
                for (Path source : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String code = Files.readString(source);
                    var packageMatcher = Pattern.compile("package ([^;]+);").matcher(code);
                    assertThat(packageMatcher.find()).as(source.toString()).isTrue();
                    String packageName = packageMatcher.group(1);
                    String declaredPath = packageName.replace('.', '/') + "/" + source.getFileName();
                    assertThat(sourceRoot.relativize(source).toString().replace('\\', '/')).isEqualTo(declaredPath);
                    if (module.equals("cyys-admin")) {
                        assertThat(packageName).as(source.toString())
                                .matches("com\\.cyys\\.admin\\.[a-z]+\\.(controller|model|dto|service|mapper)");
                    }
                    if (packageName.endsWith(".controller")) {
                        assertThat(code).as(source.toString()).doesNotContain(".mapper.", ".model.");
                    }
                    if (module.equals("cyys-common")) {
                        assertThat(code).as(source.toString()).doesNotContain("com.cyys.admin.", "com.cyys.modules.");
                    }
                    assertThat(code).as("未接入受控边界的持久化绕过路径: " + source)
                            .doesNotContain("JdbcTemplate", "DriverManager", "getConnection(", "prepareStatement(",
                                    "@InterceptorIgnore", "@CacheNamespace", "StatementType.CALLABLE");
                }
            }
        }
    }
}
