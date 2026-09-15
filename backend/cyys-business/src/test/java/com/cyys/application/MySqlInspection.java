package com.cyys.application;

import java.sql.DriverManager;

/** 本次现有数据库的只读核对工具。 */
public class MySqlInspection {
    public static void main(String[] args) throws Exception {
        String url = System.getenv("CYYS_TEST_DB_URL");
        try (var connection = DriverManager.getConnection(url,
                System.getenv("CYYS_TEST_DB_USERNAME"), System.getenv("CYYS_TEST_DB_PASSWORD"));
             var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("select version()")) {
                while (rows.next()) System.out.println("MYSQL=" + rows.getString(1));
            }
            try (var rows = statement.executeQuery("select database(), count(*) from information_schema.tables where table_schema=database()")) {
                while (rows.next()) System.out.println("DATABASE=" + rows.getString(1) + " TABLES=" + rows.getLong(2));
            }
            try (var rows = statement.executeQuery("select table_schema,table_name,table_rows from information_schema.tables where table_schema=database() order by table_name")) {
                while (rows.next()) System.out.println("TABLE=" + rows.getString(1) + "." + rows.getString(2) + " APPROX_ROWS=" + rows.getLong(3));
            }
            if (args.length == 1 && args[0].equals("--verify-cyys")) {
                for (String table : java.util.List.of("sys_scope", "sys_user", "sys_user_scope", "sys_role",
                        "sys_user_role", "sys_menu", "sys_role_menu", "sys_org", "sys_role_org", "sys_data_audit")) {
                    try (var rows = statement.executeQuery("select count(*) from " + table)) {
                        rows.next();
                        System.out.println("EXACT_ROWS=" + table + ":" + rows.getLong(1));
                    }
                }
                try (var rows = statement.executeQuery("select version,script,success from flyway_schema_history order by installed_rank")) {
                    while (rows.next()) System.out.println("MIGRATION=" + rows.getString(1) + ":" + rows.getString(2) + ":" + rows.getInt(3));
                }
                try (var rows = statement.executeQuery("select count(*) from information_schema.referential_constraints where constraint_schema=database()")) {
                    rows.next(); System.out.println("FOREIGN_KEYS=" + rows.getLong(1));
                }
            }
        }
    }
}
