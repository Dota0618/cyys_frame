package com.cyys.application;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/** 复用本机开发测试库，检查失败迁移和固定夹具冲突，允许已有业务数据及新增业务表。 */
public class MySqlTestDatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final String USERS = "('u-alice','u-bob','u-platform','g1-legacy-account')";
    private static final String SCOPES = "('scope-a','scope-b','scope-disabled')";

    @Override public void initialize(ConfigurableApplicationContext context) {
        var environment = context.getEnvironment();
        String url = environment.getRequiredProperty("spring.datasource.url");
        if (!url.matches("jdbc:mysql://(?:localhost|127\\.0\\.0\\.1):[0-9]+/[A-Za-z0-9_]+(?:\\?.*)?")
                || url.matches("(?is).*?[?&](?:user|password)=.*"))
            throw new IllegalStateException("测试必须指定本机 MySQL 开发测试库，连接地址不能包含凭据。");
        try (var connection = DriverManager.getConnection(url,
                environment.getRequiredProperty("spring.datasource.username"), environment.getRequiredProperty("spring.datasource.password"))) {
            var tables = new LinkedHashMap<String,Set<String>>();
            try (var query = connection.prepareStatement("select table_name,column_name from information_schema.columns where table_schema=?")) {
                query.setString(1, connection.getCatalog());
                try (var rows = query.executeQuery()) {
                    while (rows.next()) tables.computeIfAbsent(rows.getString(1), ignored -> new HashSet<>()).add(rows.getString(2));
                }
            }
            if (tables.containsKey("flyway_schema_history")) {
                try (var statement = connection.createStatement(); var rows = statement.executeQuery("select 1 from flyway_schema_history where success=0 limit 1")) {
                    if (rows.next()) throw new IllegalStateException("开发测试库存在失败迁移，需先核对实际执行状态。");
                }
            }
            for (var entry : tables.entrySet()) {
                String table = entry.getKey();
                if (!table.matches("(?:sys|param)_[A-Za-z0-9_]+")) continue;
                var columns = entry.getValue();
                var conditions = new ArrayList<String>();
                if (columns.contains("id")) conditions.add("id in ('u-alice','u-bob','u-platform','scope-a','scope-b','scope-disabled',"
                        + "'role-a','role-b','role-platform','permission-read','permission-write','platform-access','platform-read','bad',"
                        + "'98101','98111','98301','98311','98401','98411','990000000001','990000000002') or id like 'g1-%' or id like 'g4-%'");
                if (table.equals("sys_user")) conditions.add("login_name in ('alice','bob','platform','system-admin','old-root') or login_name like 'g4-%'");
                if (table.equals("sys_scope")) conditions.add("code in ('unit-a','unit-b','unit-disabled')");
                if (columns.contains("scope_id")) conditions.add("scope_id in " + SCOPES);
                for (String column : List.of("user_id", "created_by", "actor_id"))
                    if (columns.contains(column)) conditions.add(column + " in " + USERS);
                if (columns.contains("name") && table.equals("sys_configure")) conditions.add("name like 'g4-config-%' or name like 'g4-browser-global-%'");
                if (columns.contains("system_name")) conditions.add("system_name like 'g4-browser-global-%'");
                if (columns.contains("code")) conditions.add("code like 'g4p:%' or code like 'G4_BROWSER_%' or code='g4-browser-menu'");
                if (table.equals("sys_login_audit")) conditions.add("login_name in ('unknown','g4-audit-unknown') or login_name like 'g4-%'");
                if (conditions.isEmpty()) continue;
                try (var statement = connection.createStatement(); var rows = statement.executeQuery(
                        "select 1 from `" + table + "` where " + String.join(" or ", conditions) + " limit 1")) {
                    if (rows.next()) throw new IllegalStateException("测试夹具标识已被占用：" + table + "；不会清理现有记录。");
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("开发测试库连接、迁移或夹具检查失败，尚未执行测试写入。", exception);
        }
    }
}
