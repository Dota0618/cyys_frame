package com.cyys.admin.cache.mapper;

import com.cyys.admin.cache.model.CacheDefinition;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
@ManualDataScope
public interface CacheDefinitionMapper {
    @Select("select id,name,cache_key,group_name,refresh_group,scope,description,version "
            + "from sys_cache_definition where deleted=0 order by group_name,refresh_group,name,id")
    List<CacheDefinition> list();

    @Select("select id,name,cache_key,group_name,refresh_group,scope,description,version "
            + "from sys_cache_definition where id=#{id} and deleted=0")
    CacheDefinition detail(String id);

    @Select("select id,name,cache_key,group_name,refresh_group,scope,description,version "
            + "from sys_cache_definition where refresh_group=#{refreshGroup} and scope=#{scope} and deleted=0 "
            + "order by name,id")
    List<CacheDefinition> refreshGroup(@Param("refreshGroup") String refreshGroup, @Param("scope") String scope);

    @Insert("insert into sys_cache_definition(id,name,cache_key,group_name,refresh_group,scope,description,created_by,updated_by) "
            + "values(#{row.id},#{row.name},#{row.cacheKey},#{row.groupName},#{row.refreshGroup},#{row.scope},#{row.description},#{actor},#{actor})")
    int create(@Param("row") CacheDefinition row, @Param("actor") String actor);

    @Update("update sys_cache_definition set name=#{row.name},cache_key=#{row.cacheKey},group_name=#{row.groupName},"
            + "refresh_group=#{row.refreshGroup},scope=#{row.scope},description=#{row.description},version=version+1,"
            + "updated_by=#{actor},updated_at=CURRENT_TIMESTAMP where id=#{row.id} and version=#{row.version} and deleted=0")
    int update(@Param("row") CacheDefinition row, @Param("actor") String actor);

    @Update("update sys_cache_definition set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=0")
    int delete(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);
}
