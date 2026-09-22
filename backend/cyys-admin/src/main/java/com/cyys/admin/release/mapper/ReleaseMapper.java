package com.cyys.admin.release.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.release.model.Release;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;

@Mapper
@ManualDataScope
public interface ReleaseMapper {
    String COLUMNS = "id,release_number,date_update,type,type_label,content,version,created_at";

    @Select("<script>select " + COLUMNS + " from sys_version where deleted=0 "
            + "<if test='number != null and number != &quot;&quot;'>and release_number like concat('%',#{number},'%')</if> "
            + "order by created_at desc,id desc</script>")
    IPage<Release> page(Page<Release> page, @Param("number") String number);

    @Select("select " + COLUMNS + " from sys_version where id=#{id} and deleted=0")
    Release detail(String id);

    @Select("select " + COLUMNS + " from sys_version where id=#{id} and deleted=0 for update")
    Release lock(String id);

    @Insert("insert into sys_version(id,release_number,date_update,type,type_label,content,created_by,updated_by) "
            + "values(#{row.id},#{row.releaseNumber},#{row.dateUpdate},#{row.type},#{row.typeLabel},#{row.content},#{actor},#{actor})")
    int create(@Param("row") Release row, @Param("actor") String actor);

    @Update("update sys_version set release_number=#{row.releaseNumber},date_update=#{row.dateUpdate},type=#{row.type},"
            + "type_label=#{row.typeLabel},content=#{row.content},version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{row.id} and version=#{row.version} and deleted=0")
    int update(@Param("row") Release row, @Param("actor") String actor);

    @Update("update sys_version set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=0")
    int softDelete(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);

    @Select("<script>select id,release_number as title,null as scope_id,null as scope_name,null as department_id,null as department_name,updated_by as deleted_by,null as deleted_by_name,updated_at as deleted_at,created_at,version from sys_version where deleted=1 "
            + "<if test='search!=null and search!=&quot;&quot;'>and release_number like concat('%',#{search},'%')</if> "
            + "order by created_at desc,id desc</script>")
    com.baomidou.mybatisplus.core.metadata.IPage<com.cyys.admin.recycle.dto.RecycleRecord> recycled(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.cyys.admin.recycle.dto.RecycleRecord> page,
            @Param("search") String search);

    @Select("select " + COLUMNS + " from sys_version where id=#{id} and deleted=1 for update")
    Release lockRecycled(String id);

    @Update("update sys_version set deleted=0,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=1")
    int restore(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);

    @Delete("delete from sys_version where id=#{id} and version=#{version} and deleted=1")
    int purge(@Param("id") String id, @Param("version") int version);
}
