package com.cyys.application;

import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.mybatis.ManualDataScope;
import com.cyys.common.mybatis.ProtectedMapper;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;

/** 对已批准的目录与依赖边界做轻量检查，不引入额外架构测试框架。 */
class ArchitectureRulesTest {
    @Test
    void modulePackagesAndImportsFollowProjectBoundaries() throws Exception {
        Path backend = Path.of("..").toAbsolutePath().normalize();
        List<String> modules = moduleNames(backend);
        for (String module : modules) {
            Path sourceRoot = backend.resolve(module + "/src/main/java");
            if (!Files.isDirectory(sourceRoot)) continue;
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
                    String prefix = "com.cyys." + module.substring("cyys-".length());
                    assertThat(packageName).as("包名必须对应所属模块: " + source)
                            .matches(Pattern.quote(prefix) + "(?:\\.[a-z][a-z0-9]*)*");
                    if (!module.equals("cyys-application")) {
                        assertThat(code).as("库模块不得依赖启动模块: " + source)
                                .doesNotContain("\nimport com.cyys.application.");
                    }
                    if (packageName.endsWith(".controller")) {
                        assertThat(code).as(source.toString()).doesNotContain(".mapper.", ".model.");
                    }
                    if (module.equals("cyys-common")) {
                        assertThat(code).as(source.toString())
                                .doesNotContain("com.cyys.admin.", "com.cyys.modules.", "com.cyys.application.");
                    }
                    if (module.equals("cyys-common") || module.equals("cyys-admin")) {
                        for (String dependency : modules) {
                            if (dependency.equals("cyys-common") || dependency.equals("cyys-admin")) continue;
                            String dependencyPackage = "com.cyys." + dependency.substring("cyys-".length()) + ".";
                            assertThat(code).as("基础模块不得依赖具体业务或启动模块: " + source)
                                    .doesNotContain("\nimport " + dependencyPackage);
                        }
                    }
                    if (packageName.endsWith(".service")) {
                        String className = packageName + "." + source.getFileName().toString().replaceFirst("\\.java$", "");
                        for (var method : Class.forName(className).getDeclaredMethods()) {
                            if (!Modifier.isPublic(method.getModifiers()) || method.isSynthetic()) continue;
                            assertThat(method.getGenericReturnType().getTypeName())
                                    .as("Service 返回类型不得暴露 Mapper 内部结构: " + method)
                                    .doesNotContain(".mapper.");
                        }
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
        Path backend = Path.of("..").toAbsolutePath().normalize();
        DataPolicyRegistry registry = new DataPolicyRegistry();
        for (String module : moduleNames(backend)) {
            Path sourceRoot = backend.resolve(module + "/src/main/java");
            if (!Files.isDirectory(sourceRoot)) continue;
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

    private static List<String> moduleNames(Path backend) throws Exception {
        var factory = javax.xml.parsers.DocumentBuilderFactory.newDefaultInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        var document = factory.newDocumentBuilder().parse(backend.resolve("pom.xml").toFile());
        var declarations = document.getElementsByTagName("module");
        var modules = new java.util.ArrayList<String>();
        for (int index = 0; index < declarations.getLength(); index++) {
            modules.add(declarations.item(index).getTextContent().strip());
        }
        return modules;
    }
}
