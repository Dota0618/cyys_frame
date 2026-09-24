package com.cyys.admin.cache.mapper;

import com.cyys.admin.cache.model.CacheRefreshItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CacheRefreshItemMapper {
    @Select("select id,name,version from sys_cache_refresh where deleted=0 order by name,id")
    List<CacheRefreshItem> list();

    @Select("select id,name,version from sys_cache_refresh where id=#{id} and deleted=0")
    CacheRefreshItem detail(String id);

    @Insert("insert into sys_cache_refresh(id,name,created_by,updated_by) values(#{row.id},#{row.name},#{actor},#{actor})")
    int create(@Param("row") CacheRefreshItem row, @Param("actor") String actor);

    @Update("update sys_cache_refresh set name=#{row.name},version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{row.id} and version=#{row.version} and deleted=0")
    int update(@Param("row") CacheRefreshItem row, @Param("actor") String actor);

    @Update("update sys_cache_refresh set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=0")
    int delete(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);
}
