package com.cyys.application;

import com.cyys.admin.file.mapper.FileCleanupMapper;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** 正式迁移后挂载测试机构，恢复本轮修改的共享菜单，不替换已有总单位。 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestFixtures {
    @Bean FlywayMigrationStrategy testScopeFixtures() {
        return flyway -> {
            flyway.migrate();
            var jdbc = new JdbcTemplate(flyway.getConfiguration().getDataSource());
            new TransactionTemplate(new JdbcTransactionManager(Objects.requireNonNull(jdbc.getDataSource()))).executeWithoutResult(status -> {
                var roots = jdbc.queryForList("select id from sys_scope where parent_id='0' and deleted=0", String.class);
                String parent = roots.isEmpty() ? "0" : roots.getFirst();
                String ancestors = parent.equals("0") ? "" : ancestorPath(jdbc, parent);
                jdbc.update("insert into sys_scope(id,code,name,status,remark,parent_id,ancestors) values "
                        + "('scope-a','unit-a','总单位 A',1,'CYYS automated test fixture',?,?)", parent, ancestors);
                String childrenPath = ancestorPath(jdbc, "scope-a");
                jdbc.update("insert into sys_scope(id,code,name,status,remark,parent_id,ancestors) values "
                        + "('scope-b','unit-b','单位 B',1,'CYYS automated test fixture','scope-a',?),"
                        + "('scope-disabled','unit-disabled','已禁用单位',0,'CYYS automated test fixture','scope-a',?)", childrenPath, childrenPath);
            });
        };
    }

    static String ancestorPath(JdbcTemplate jdbc, String scope) {
        return jdbc.queryForObject("select concat_ws(',',nullif(ancestors,''),id) from sys_scope where id=?", String.class, scope);
    }

    static String parent(JdbcTemplate jdbc, String scope) {
        return jdbc.queryForObject("select parent_id from sys_scope where id=?", String.class, scope);
    }

    static void restoreMenus(JdbcTemplate jdbc, List<Map<String,Object>> original) {
        if (original == null) return;
        new TransactionTemplate(new JdbcTransactionManager(Objects.requireNonNull(jdbc.getDataSource()))).executeWithoutResult(status -> {
            for (var row : original) {
                String id = row.get("id").toString();
                var current = jdbc.queryForList("select * from sys_menu where id=?", id);
                if (!current.isEmpty() && current.getFirst().equals(row)) continue;
                var columns = new ArrayList<>(row.keySet());
                if (columns.stream().anyMatch(name -> !name.matches("[a-z_]+"))) throw new IllegalStateException("菜单字段无法恢复");
                String names = String.join(",", columns.stream().map(name -> "`" + name + "`").toList());
                String placeholders = String.join(",", Collections.nCopies(columns.size(), "?"));
                var updates = columns.stream().filter(name -> !name.equals("id")).toList();
                var values = new ArrayList<Object>();
                columns.forEach(name -> values.add(row.get(name)));
                updates.forEach(name -> values.add(row.get(name)));
                jdbc.update("insert into sys_menu(" + names + ") values(" + placeholders + ") on duplicate key update "
                        + String.join(",", updates.stream().map(name -> "`" + name + "`=?").toList()), values.toArray());
            }
        });
    }

    static void restrictFileCleanup(FileCleanupMapper mapper) {
        doAnswer(invocation -> {
            LocalDateTime now = invocation.getArgument(1);
            int limit = invocation.getArgument(2);
            var candidates = new ArrayList<FileCleanupMapper.Candidate>();
            for (String scope : List.of("scope-a", "scope-b")) candidates.addAll(mapper.candidates(scope, now, limit));
            return candidates.stream().limit(limit).toList();
        }).when(mapper).candidates(isNull(), any(LocalDateTime.class), anyInt());
    }
}
