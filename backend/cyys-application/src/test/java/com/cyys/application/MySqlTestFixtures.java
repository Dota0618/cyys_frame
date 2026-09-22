package com.cyys.application;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/** 正式迁移后准备一个总单位及下级机构，不占用迁移版本。 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlTestFixtures {
    @Bean
    FlywayMigrationStrategy testScopeFixtures() {
        return flyway -> {
            flyway.migrate();
            var jdbc = new JdbcTemplate(flyway.getConfiguration().getDataSource());
            jdbc.update("insert into sys_scope(id,code,name,status,remark,parent_id,ancestors) values "
                    + "('scope-a','unit-a','总单位 A',1,'CYYS automated test fixture','0',''),"
                    + "('scope-b','unit-b','单位 B',1,'CYYS automated test fixture','scope-a','scope-a'),"
                    + "('scope-disabled','unit-disabled','已禁用单位',0,'CYYS automated test fixture','scope-a','scope-a')");
        };
    }
}
