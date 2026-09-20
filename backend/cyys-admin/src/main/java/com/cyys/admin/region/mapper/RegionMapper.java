package com.cyys.admin.region.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.region.model.Region;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;

@Mapper
@ManualDataScope
public interface RegionMapper {
    String COLUMNS="r.id,r.parent_id,r.city_code,r.name,r.short_name,r.zip_code,r.longitude,r.latitude,r.level,r.sort,r.version";
    String FILTER="from param_region r where r.deleted=0 "
            + "<choose><when test='parent != null'>and r.parent_id=#{parent}</when><otherwise>and r.parent_id is null</otherwise></choose> "
            + "<if test='name != null and name != &quot;&quot;'>and r.name like concat('%',#{name},'%')</if> ";

    @Select("<script>select " + COLUMNS + ",exists(select 1 from param_region c where c.parent_id=r.id and c.deleted=0) as has_children "
            + FILTER
            + "order by r.sort,r.id</script>")
    IPage<Region> page(Page<Region> page,@Param("parent") String parent,@Param("name") String name);

    @Select("<script>select " + COLUMNS + " " + FILTER + "order by r.sort,r.id limit #{limit}</script>")
    @Options(timeout = 15)
    java.util.List<Region> export(@Param("parent") String parent, @Param("name") String name, @Param("limit") int limit);

    @Select("select " + COLUMNS + ",exists(select 1 from param_region c where c.parent_id=r.id and c.deleted=0) as has_children "
            + "from param_region r where r.id=#{id} and r.deleted=0")
    Region detail(String id);

    @Select("select " + COLUMNS + " from param_region r where r.id=#{id} and r.deleted=0 for update")
    Region lock(String id);

    @Select("select id from param_region where parent_id=#{id} and deleted=0 order by sort,id limit 1 for update")
    String firstChild(String id);

    @Insert("insert into param_region(id,parent_id,city_code,name,short_name,zip_code,longitude,latitude,level,sort,created_by,updated_by) "
            + "values(#{row.id},#{row.parentId},#{row.cityCode},#{row.name},#{row.shortName},#{row.zipCode},#{row.longitude},#{row.latitude},#{row.level},#{row.sort},#{actor},#{actor})")
    int create(@Param("row") Region row,@Param("actor") String actor);

    @Update("update param_region set city_code=#{row.cityCode},name=#{row.name},short_name=#{row.shortName},zip_code=#{row.zipCode},"
            + "longitude=#{row.longitude},latitude=#{row.latitude},sort=#{row.sort},version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{row.id} and version=#{row.version} and deleted=0")
    int update(@Param("row") Region row,@Param("actor") String actor);

    @Update("update param_region set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=0")
    int softDelete(@Param("id") String id,@Param("version") int version,@Param("actor") String actor);
}
