package com.cyys.admin.generator.mapper;

import com.cyys.admin.generator.dto.*;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** Platform-only metadata in the current schema. No caller-selected schema, SQL or row values. */
@Mapper
@ManualDataScope
public interface GeneratorMetadataMapper {
    @Select("select table_name as name,table_comment as comment from information_schema.tables " +
            "where table_schema=database() and table_type='BASE TABLE' and left(table_name,4)='biz_' order by table_name limit 201")
    List<GeneratorTable> tables();

    @Select("select column_name as name,data_type,column_type,character_maximum_length as length,numeric_scale as scale," +
            "(is_nullable='YES') as nullable,column_key as key_type,extra,column_comment as comment " +
            "from information_schema.columns where table_schema=database() and table_name=#{table} order by ordinal_position limit 101")
    List<GeneratorColumn> columns(@Param("table") String table);
}
