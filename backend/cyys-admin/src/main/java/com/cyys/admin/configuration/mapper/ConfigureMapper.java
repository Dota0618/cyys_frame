package com.cyys.admin.configuration.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.configuration.model.Configure;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;

@Mapper
@ManualDataScope
public interface ConfigureMapper {
    @Select("""
            <script>
            select id,name,`value`,type,desp,sort,version from sys_configure where deleted=0
            <if test="name != null and name != ''">and name like concat('%',#{name},'%')</if>
            order by sort,id
            </script>
            """)
    IPage<Configure> page(Page<Configure> page, @Param("name") String name);

    @Select("select id,name,`value`,type,desp,sort,version from sys_configure where id=#{id} and deleted=0")
    Configure detail(String id);

    @Insert("insert into sys_configure(id,name,`value`,type,desp,sort,created_by,updated_by) "
            + "values(#{row.id},#{row.name},#{row.value},#{row.type},#{row.desp},#{row.sort},#{actor},#{actor})")
    int create(@Param("row") Configure row, @Param("actor") String actor);

    // 显式写入 value，包括空字符串和 null，避免 ORM 忽略清空动作。
    @Update("update sys_configure set `value`=#{row.value},type=#{row.type},desp=#{row.desp},sort=#{row.sort},"
            + "version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{row.id} and version=#{row.version} and deleted=0")
    int update(@Param("row") Configure row, @Param("actor") String actor);

    @Update("update sys_configure set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=0")
    int softDelete(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);
}
