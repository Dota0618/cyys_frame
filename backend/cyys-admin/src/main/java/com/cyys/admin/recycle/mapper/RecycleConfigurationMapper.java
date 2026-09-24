package com.cyys.admin.recycle.mapper;

import com.cyys.admin.recycle.model.RecycleConfiguration;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
@ManualDataScope
public interface RecycleConfigurationMapper {
    String COLUMNS = "id,code,name,table_name,title_column,search_column,scope_required,scope_column,"
            + "department_column,ancestors_column,actor_column,version_column,deleted_column,created_at_column,"
            + "updated_at_column,updated_by_column,sort,version,enabled,created_by,created_at,updated_by,updated_at";

    @Select("select " + COLUMNS + " from sys_recycle_resource where deleted=0 order by sort,code")
    List<RecycleConfiguration> all();

    @Select("select " + COLUMNS + " from sys_recycle_resource where id=#{id} and deleted=0")
    RecycleConfiguration detail(@Param("id") String id);

    @Select("select " + COLUMNS + " from sys_recycle_resource where code=#{code} and deleted=0 and enabled=1")
    RecycleConfiguration enabledByCode(@Param("code") String code);

    @Select("select " + COLUMNS + " from sys_recycle_resource where code=#{code} and deleted=0 for update")
    RecycleConfiguration lockByCode(@Param("code") String code);

    @Select("select id from sys_recycle_resource where code=#{code}")
    String idByCode(@Param("code") String code);

    @Select("select id from sys_recycle_resource where table_name=#{tableName} and deleted=0")
    String idByTable(@Param("tableName") String tableName);

    @Select("select table_name from information_schema.tables where table_schema=database() and table_name=#{tableName}")
    String existingTable(@Param("tableName") String tableName);

    @Select("select column_name from information_schema.columns where table_schema=database() and table_name=#{tableName}")
    List<String> columns(@Param("tableName") String tableName);

    @Select("select t.table_name,group_concat(concat(c.column_name,':',c.data_type,':',if(c.is_nullable='YES','1','0')) order by c.ordinal_position separator ',') as column_info "
            + "from information_schema.tables t join information_schema.columns c on c.table_schema=t.table_schema and c.table_name=t.table_name "
            + "where t.table_schema=database() and t.table_type='BASE TABLE' "
            + "group by t.table_name order by t.table_name")
    List<TableMetadataRow> metadata();

    @Insert("insert into sys_recycle_resource(id,code,name,table_name,title_column,search_column,scope_required,scope_column,department_column,ancestors_column,actor_column,version_column,deleted_column,created_at_column,updated_at_column,updated_by_column,sort,enabled,created_by,updated_by) "
            + "values(#{row.id},#{row.code},#{row.name},#{row.tableName},#{row.titleColumn},#{row.searchColumn},#{row.scopeRequired},#{row.scopeColumn},#{row.departmentColumn},#{row.ancestorsColumn},#{row.actorColumn},#{row.versionColumn},#{row.deletedColumn},#{row.createdAtColumn},#{row.updatedAtColumn},#{row.updatedByColumn},#{row.sort},#{row.enabled},#{actor},#{actor})")
    int create(@Param("row") RecycleConfiguration row, @Param("actor") String actor);

    @Update("update sys_recycle_resource set name=#{row.name},title_column=#{row.titleColumn},search_column=#{row.searchColumn},scope_required=#{row.scopeRequired},scope_column=#{row.scopeColumn},department_column=#{row.departmentColumn},ancestors_column=#{row.ancestorsColumn},actor_column=#{row.actorColumn},version_column=#{row.versionColumn},deleted_column=#{row.deletedColumn},created_at_column=#{row.createdAtColumn},updated_at_column=#{row.updatedAtColumn},updated_by_column=#{row.updatedByColumn},sort=#{row.sort},enabled=#{row.enabled},version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP where id=#{row.id} and version=#{row.version} and deleted=0")
    int update(@Param("row") RecycleConfiguration row, @Param("actor") String actor);

    @Update("update sys_recycle_resource set deleted=1,enabled=0,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP where id=#{id} and version=#{version} and deleted=0")
    int softDelete(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);

    record TableMetadataRow(String tableName, String columnInfo) {}
}
