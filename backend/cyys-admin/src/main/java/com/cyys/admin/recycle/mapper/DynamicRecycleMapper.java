package com.cyys.admin.recycle.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.recycle.dto.RecycleRecord;
import com.cyys.admin.recycle.model.RecycleConfiguration;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;

/** 执行后台登记资源生成的受控 SQL；所有表名和字段名先由服务端校验。 */
@Mapper
@ManualDataScope
public interface DynamicRecycleMapper {
    @SelectProvider(type = DynamicRecycleSqlProvider.class, method = "page")
    IPage<RecycleRecord> page(Page<RecycleRecord> page,
                              @Param("definition") RecycleConfiguration definition,
                              @Param("predicate") String predicate,
                              @Param("search") String search);

    @SelectProvider(type = DynamicRecycleSqlProvider.class, method = "lock")
    DynamicRecycleRow lock(@Param("definition") RecycleConfiguration definition,
                           @Param("predicate") String predicate,
                           @Param("id") String id);

    @UpdateProvider(type = DynamicRecycleSqlProvider.class, method = "restore")
    int restore(@Param("definition") RecycleConfiguration definition,
                @Param("predicate") String predicate,
                @Param("id") String id,
                @Param("version") int version,
                @Param("actor") String actor);

    @DeleteProvider(type = DynamicRecycleSqlProvider.class, method = "purge")
    int purge(@Param("definition") RecycleConfiguration definition,
              @Param("predicate") String predicate,
              @Param("id") String id,
              @Param("version") int version);

    record DynamicRecycleRow(String id, Integer version) {}
}
