package com.cyys.application;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** 在 Flyway 和测试数据写入之前，仅接受空库或只含原始初始化数据的测试库。 */
public class MySqlTestDatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Set<String> TABLES = Set.of(
            "sys_scope", "sys_user", "sys_user_scope", "sys_user_scope_org", "sys_role", "sys_user_role",
            "sys_menu", "sys_role_menu", "sys_org", "sys_data_audit", "sys_login_audit", "sys_configure",
            "param_basic", "param_basic_type", "param_region", "sys_file_entry", "sys_file_part", "sys_file_dir",
            "sys_file_binding", "sys_org_notice", "sys_org_notice_ref", "sys_notice", "sys_version",
            "sys_serial_rule", "sys_serial_counter", "sys_recycle_resource", "sys_cache_definition",
            "flyway_schema_history");
    private static final Pattern INSERT = Pattern.compile("INSERT INTO `(\\w+)` \\((.+)\\) VALUES");
    private static final Pattern VALUE = Pattern.compile("'((?:''|\\\\.|[^'])*)'|\\bNULL\\b|-?\\d+");

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        var environment = context.getEnvironment();
        String url = environment.getRequiredProperty("CYYS_TEST_DB_URL");
        if (!url.startsWith("jdbc:mysql:")) {
            throw new IllegalStateException("测试只支持明确指定的现有 MySQL schema。");
        }
        try (var connection = DriverManager.getConnection(url,
                environment.getRequiredProperty("CYYS_TEST_DB_USERNAME"),
                environment.getRequiredProperty("CYYS_TEST_DB_PASSWORD"))) {
            String schema = connection.getCatalog();
            if (schema == null || schema.isBlank()) {
                throw new IllegalStateException("CYYS_TEST_DB_URL 必须指定数据库名。");
            }
            Set<String> actualTables = new HashSet<>();
            try (var query = connection.prepareStatement(
                    "select table_name from information_schema.tables where table_schema=?")) {
                query.setString(1, schema);
                try (var rows = query.executeQuery()) {
                    while (rows.next()) actualTables.add(rows.getString(1));
                }
            }
            if (actualTables.isEmpty()) return;
            if (!actualTables.equals(TABLES)) {
                throw new IllegalStateException("测试拒绝未知或未纳管的数据库结构；不会删表或自动 baseline。");
            }
            var seeds = initializationRows();
            try (var statement = connection.createStatement()) {
                for (String table : actualTables) {
                    if (table.equals("flyway_schema_history")) continue;
                    var expected = seeds.getOrDefault(table, Map.of());
                    try (var rows = statement.executeQuery("select * from " + table)) {
                        while (rows.next()) {
                            var seed = expected.get(rows.getString("id"));
                            boolean matches = seed != null;
                            if (matches) {
                                for (var field : seed.entrySet()) {
                                    if (!java.util.Objects.equals(field.getValue(), rows.getString(field.getKey()))) {
                                        matches = false;
                                        break;
                                    }
                                }
                            }
                            if (!matches) {
                                throw new IllegalStateException("测试拒绝含已有数据或已修改初始化数据的 schema（" + table
                                        + "）；不会清空业务数据或自动处理残留测试记录。");
                            }
                        }
                    }
                }
            }
        } catch (SQLException | IOException exception) {
            throw new IllegalStateException("MySQL 测试库预检失败；请核对目标、连接和结构，尚未执行测试写入。",
                    exception);
        }
    }

    /** 初始化数据直接取自唯一 V1，避免另行维护一份种子清单。 */
    private static Map<String, Map<String, Map<String, String>>> initializationRows() throws IOException {
        var result = new HashMap<String, Map<String, Map<String, String>>>();
        String table = null;
        List<String> columns = List.of();
        var resource = new ClassPathResource("db/migration/system/V1__init_system_schema.sql");
        for (String line : resource.getContentAsString(StandardCharsets.UTF_8).lines().toList()) {
            var insert = INSERT.matcher(line);
            if (insert.matches()) {
                table = insert.group(1);
                columns = List.of(insert.group(2).replace("`", "").split(", "));
                result.put(table, new HashMap<>());
            } else if (table != null && line.startsWith("    (")) {
                var values = new ArrayList<String>();
                var matcher = VALUE.matcher(line);
                while (matcher.find()) {
                    values.add(matcher.group(1) != null
                            ? matcher.group(1).replace("''", "'").replace("\\\\", "\\")
                            : matcher.group().equals("NULL") ? null : matcher.group());
                }
                if (values.size() != columns.size()) {
                    throw new IOException("无法读取 V1 初始化行：" + table);
                }
                var row = new HashMap<String, String>();
                for (int i = 0; i < columns.size(); i++) row.put(columns.get(i), values.get(i));
                result.get(table).put(row.get("id"), row);
                if (line.endsWith(";")) table = null;
            }
        }
        if (result.size() != 6) throw new IOException("V1 初始化数据清单不完整。");
        return result;
    }
}
