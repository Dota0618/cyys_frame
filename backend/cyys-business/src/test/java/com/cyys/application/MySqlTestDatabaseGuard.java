package com.cyys.application;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

/** 在 Flyway 和测试数据写入之前，拒绝业务库、未知表结构及未清理的失败测试。 */
public class MySqlTestDatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Set<String> TABLES = Set.of("sys_scope", "sys_user", "sys_user_scope",
            "sys_role", "sys_user_role", "sys_menu", "sys_role_menu", "sys_org", "flyway_schema_history");
    private static final Set<String> PHASE3_TABLES = Set.of("sys_role_org", "sys_data_audit");

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
            var upgradedTables = new HashSet<>(TABLES);
            upgradedTables.addAll(PHASE3_TABLES);
            if (!actualTables.equals(TABLES) && !actualTables.equals(upgradedTables)) {
                throw new IllegalStateException("测试拒绝未知或未纳管的数据库结构；不会删表或自动 baseline。");
            }
            try (var statement = connection.createStatement()) {
                for (String table : actualTables) {
                    if (table.equals("flyway_schema_history")) continue;
                    try (var rows = statement.executeQuery("select 1 from " + table + " limit 1")) {
                        if (rows.next()) {
                            throw new IllegalStateException("测试拒绝含已有数据的 schema（" + table
                                    + "）；不会清空业务数据或自动处理残留测试记录。");
                        }
                    }
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("MySQL 测试库预检失败；请核对目标、连接和结构，尚未执行测试写入。",
                    exception);
        }
    }
}
