package com.cyys.application;

import cn.dev33.satoken.secure.BCrypt;
import java.sql.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** 手工本机 Docker 演练专用；不进入发布 JAR，不接受任意表、SQL 或现有业务数据。 */
public final class LocalDeploymentFixture {
    private static final String USER = "rehearsal-user";
    private static final String SCOPES = "('rehearsal-a','rehearsal-b')";
    private static final String[] PERMISSIONS = {"platform:scope:access", "platform:data:operate",
            "platform:data:read-all", "user:read", "user:export", "file:upload", "file:read",
            "file:content:read", "department:read"};
    private static final Map<String,String> OWNED = new LinkedHashMap<>();
    static {
        OWNED.put("sys_file_part", "actor_id='rehearsal-user' and scope_id in " + SCOPES);
        OWNED.put("sys_file_entry", "created_by='rehearsal-user' and scope_id in " + SCOPES);
        OWNED.put("sys_data_audit", "actor_id='rehearsal-user' and scope_id in ('GLOBAL','rehearsal-a','rehearsal-b')");
        OWNED.put("sys_login_audit", "user_id='rehearsal-user' and login_name='local-rehearsal'");
        OWNED.put("sys_user_role", "id='rehearsal-grant' and user_id='rehearsal-user' and role_id='rehearsal-role' and scope_id='GLOBAL'");
        OWNED.put("sys_role_menu", "role_id='rehearsal-role' and menu_id in (" + menuIds() + ")");
        OWNED.put("sys_menu", "id in (" + menuIds() + ") and created_by='rehearsal-user'");
        OWNED.put("sys_role", "id='rehearsal-role' and scope_id='GLOBAL' and code='LOCAL_REHEARSAL'");
        OWNED.put("sys_user_scope", "user_id='rehearsal-user' and scope_id in " + SCOPES);
        OWNED.put("sys_org", "id in ('rehearsal-org-a','rehearsal-org-b') and scope_id in " + SCOPES + " and created_by='rehearsal-user'");
        OWNED.put("sys_user", "id='rehearsal-user' and login_name='local-rehearsal'");
        OWNED.put("sys_scope", "id in " + SCOPES + " and remark='CYYS local Docker rehearsal'");
        for (String table : List.of("sys_user_scope_org", "sys_configure", "param_basic", "param_basic_type",
                "sys_file_dir", "sys_org_notice", "sys_org_notice_ref", "sys_file_binding", "sys_notice",
                "sys_version", "param_region", "sys_serial_rule", "sys_serial_counter")) OWNED.put(table, "1=0");
    }
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !Set.of("seed", "clear", "snapshot").contains(args[0])) throw new IllegalArgumentException("seed|clear|snapshot");
        try (var c = DriverManager.getConnection(required("CYYS_TEST_DB_URL"), required("CYYS_TEST_DB_USERNAME"), required("CYYS_TEST_DB_PASSWORD"))) {
            if (!"cyys_frame".equals(c.getCatalog())) throw new IllegalStateException("Only the selected local test schema is allowed");
            c.setAutoCommit(false);
            guard(c, args[0].equals("seed"));
            switch (args[0]) {
                case "seed" -> seed(c);
                case "clear" -> {
                    for (var entry : OWNED.entrySet()) execute(c, "delete from " + entry.getKey() + " where " + entry.getValue());
                }
                case "snapshot" -> snapshot(c);
            }
            c.commit();
            if (!args[0].equals("snapshot")) System.out.println("Local rehearsal " + args[0] + " completed");
        }
    }
    private static void guard(Connection c, boolean empty) throws SQLException {
        var actual = new TreeSet<String>();
        try (var s = c.createStatement(); var r = s.executeQuery("select table_name from information_schema.tables where table_schema=database()")) {
            while (r.next()) actual.add(r.getString(1));
        }
        var expected = new TreeSet<>(OWNED.keySet()); expected.add("flyway_schema_history");
        if (!actual.equals(expected)) throw new IllegalStateException("Unknown schema; no changes made");
        for (var entry : OWNED.entrySet()) {
            String sql = "select count(*) from " + entry.getKey() + (empty ? "" : " where not (" + entry.getValue() + ") or (" + entry.getValue() + ") is null");
            try (var s = c.createStatement(); var r = s.executeQuery(sql)) {
                r.next(); if (r.getLong(1) != 0) throw new IllegalStateException("Non-rehearsal data in " + entry.getKey() + "; no changes made");
            }
        }
    }
    private static void seed(Connection c) throws SQLException {
        String hash = BCrypt.hashpw(required("CYYS_REHEARSAL_PASSWORD"), BCrypt.gensalt(10));
        execute(c, "insert into sys_user(id,login_name,password_hash,real_name) values(?,'local-rehearsal',?,'本机演练账号')", USER, hash);
        execute(c, "insert into sys_role(id,scope_id,code,name) values('rehearsal-role','GLOBAL','LOCAL_REHEARSAL','本机演练平台角色')");
        execute(c, "insert into sys_user_role(id,user_id,role_id,scope_id) values('rehearsal-grant',?,'rehearsal-role','GLOBAL')", USER);
        for (String suffix : List.of("a", "b")) {
            String scope = "rehearsal-" + suffix;
            execute(c, "insert into sys_scope(id,code,name,remark) values(?,?,?,'CYYS local Docker rehearsal')", scope, scope, "演练单位 " + suffix.toUpperCase());
            execute(c, "insert into sys_org(id,scope_id,name,code,created_by) values(?,?,?,'SAME_CODE',?)", "rehearsal-org-" + suffix, scope, "演练部门 " + suffix.toUpperCase(), USER);
            execute(c, "insert into sys_user_scope(id,user_id,scope_id,display_name,org_id,default_flag) values(?,?,?,?,?,?)",
                    "rehearsal-member-" + suffix, USER, scope, "演练成员 " + suffix.toUpperCase(), "rehearsal-org-" + suffix, suffix.equals("a") ? 1 : 0);
        }
        for (int i=0; i<PERMISSIONS.length; i++) {
            String id = "rehearsal-menu-" + i;
            execute(c, "insert into sys_menu(id,code,name,type,platform_only,created_by) values(?,?,?,3,?,?)",
                    id, PERMISSIONS[i], PERMISSIONS[i], PERMISSIONS[i].startsWith("platform:") ? 1 : 0, USER);
            execute(c, "insert into sys_role_menu(id,role_id,menu_id) values(?,'rehearsal-role',?)", "rehearsal-rm-" + i, id);
        }
    }
    private static void snapshot(Connection c) throws Exception {
        var tables = new TreeSet<>(OWNED.keySet()); tables.add("flyway_schema_history");
        for (String table : tables) {
            var values = new ArrayList<String>();
            try (var s = c.createStatement(); var r = s.executeQuery("select * from " + table)) {
                int columns = r.getMetaData().getColumnCount();
                while (r.next()) {
                    var row = new StringBuilder();
                    for (int col=1; col<=columns; col++) {
                        byte[] value = r.getBytes(col);
                        row.append(value == null ? "NULL" : Base64.getEncoder().encodeToString(value)).append(':');
                    }
                    values.add(row.toString());
                }
            }
            Collections.sort(values);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(String.join("\n",values).getBytes(StandardCharsets.UTF_8));
            System.out.println(table + " rows=" + values.size() + " sha256=" + HexFormat.of().formatHex(hash));
        }
    }
    private static String menuIds() {
        var ids = new ArrayList<String>();
        for (int i=0;i<PERMISSIONS.length;i++) ids.add("'rehearsal-menu-" + i + "'");
        return String.join(",", ids);
    }
    private static void execute(Connection c, String sql, Object... args) throws SQLException {
        try (var s=c.prepareStatement(sql)) {
            for(int i=0;i<args.length;i++) s.setObject(i+1,args[i]); s.executeUpdate();
        }
    }
    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing " + name);
        return value;
    }
}
