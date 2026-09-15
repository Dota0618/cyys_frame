package com.cyys.common.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(DataPolicyRegistry registry, RowPolicyProvider provider) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        interceptor.addInnerInterceptor(new GuardedDataPermissionInterceptor(registry, provider));
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL) {
            @Override
            protected org.apache.ibatis.mapping.MappedStatement buildCountMappedStatement(
                    org.apache.ibatis.mapping.MappedStatement mapped, String countId) {
                if (countId != null && !countId.isBlank()) {
                    throw DataPolicyRegistry.denied("自定义 COUNT 请使用独立且已登记策略的 Mapper 查询");
                }
                return null;
            }
        };
        pagination.setMaxLimit(200L);
        pagination.setOptimizeJoin(false);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
