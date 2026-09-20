package com.cyys.application;

import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.mybatis.ManualDataScope;
import com.cyys.common.mybatis.ProtectedMapper;
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
                        assertThat(code).as("业务代码不得绕过统一缓存能力: " + source)
                                .doesNotContain("org.springframework.data.redis", "StringRedisTemplate",
                                        "RedisTemplate", "DefaultRedisScript", "redis.call(");
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

    @Test
    void everyMapperDeclaresOneClassLevelDataScopeBoundary() throws Exception {
        Path sourceRoot = Path.of("..", "cyys-admin", "src", "main", "java").toAbsolutePath().normalize();
        DataPolicyRegistry registry = new DataPolicyRegistry();
        try (var paths = Files.walk(sourceRoot)) {
            for (Path source : paths.filter(path -> path.toString().endsWith("Mapper.java")).toList()) {
                String code = Files.readString(source);
                if (!code.contains("@Mapper")) continue;
                var packageMatcher = Pattern.compile("package ([^;]+);").matcher(code);
                assertThat(packageMatcher.find()).as(source.toString()).isTrue();
                String className = packageMatcher.group(1) + "."
                        + source.getFileName().toString().replaceFirst("\\.java$", "");
                Class<?> mapper = Class.forName(className);
                ProtectedMapper protectedMapper = mapper.getAnnotation(ProtectedMapper.class);
                ManualDataScope manual = mapper.getAnnotation(ManualDataScope.class);
                assertThat((protectedMapper == null) ^ (manual == null))
                        .as("Mapper 必须且只能声明一种类级数据范围边界: %s", className)
                        .isTrue();
                if (protectedMapper != null) registry.resource(protectedMapper.value());
            }
        }
    }
}
