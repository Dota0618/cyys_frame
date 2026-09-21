package com.cyys.admin.dictionary.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.dictionary.model.ParamBasic;
import com.cyys.admin.dictionary.model.ParamBasicType;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;

@Mapper
@ManualDataScope
public interface DictionaryMapper {
    @Select("""
            <script>
            select id,name,system_name,sort,version,system_builtin from param_basic where deleted=0
            <if test="name != null and name != ''">and name like concat('%',#{name},'%')</if>
            order by sort,id
            </script>
            """)
    IPage<ParamBasic> page(Page<ParamBasic> page, @Param("name") String name);

    @Select("select id,name,system_name,sort,version,system_builtin from param_basic where id=#{id} and deleted=0")
    ParamBasic detail(String id);

    // 分类删除与所有字典项写入使用同一父记录锁，防止并发插入孤立项。
    @Select("select id,name,system_name,sort,version,system_builtin from param_basic where id=#{id} and deleted=0 for update")
    ParamBasic lock(String id);

    @Insert("insert into param_basic(id,name,system_name,sort,created_by,updated_by) "
            + "values(#{row.id},#{row.name},#{row.systemName},#{row.sort},#{actor},#{actor})")
    int create(@Param("row") ParamBasic row, @Param("actor") String actor);

    @Update("update param_basic set name=#{row.name},sort=#{row.sort},version=version+1,"
            + "updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{row.id} and version=#{row.version} and deleted=0")
    int update(@Param("row") ParamBasic row, @Param("actor") String actor);

    @Select("select id from param_basic_type where basic_id=#{basicId} and deleted=0 limit 1 for update")
    String firstItem(String basicId);

    @Update("update param_basic set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=0")
    int softDelete(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);

    @Select("""
            <script>
            select id,basic_id,param_key,param_value,desp,sort,version from param_basic_type
            where basic_id=#{basicId} and deleted=0
            <if test="paramValue != null and paramValue != ''">and param_value like concat('%',#{paramValue},'%')</if>
            order by sort,id
            </script>
            """)
    IPage<ParamBasicType> items(Page<ParamBasicType> page, @Param("basicId") String basicId,
                              @Param("paramValue") String paramValue);

    @Select("select id,basic_id,param_key,param_value,desp,sort,version from param_basic_type "
            + "where basic_id=#{basicId} and id=#{id} and deleted=0")
    ParamBasicType item(@Param("basicId") String basicId, @Param("id") String id);

    @Insert("insert into param_basic_type(id,basic_id,param_key,param_value,desp,sort,created_by,updated_by) "
            + "values(#{row.id},#{row.basicId},#{row.paramKey},#{row.paramValue},#{row.desp},#{row.sort},#{actor},#{actor})")
    int createItem(@Param("row") ParamBasicType row, @Param("actor") String actor);

    @Update("update param_basic_type set param_value=#{row.paramValue},desp=#{row.desp},sort=#{row.sort},"
            + "version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{row.id} and basic_id=#{row.basicId} and version=#{row.version} and deleted=0")
    int updateItem(@Param("row") ParamBasicType row, @Param("actor") String actor);

    @Update("update param_basic_type set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and basic_id=#{basicId} and version=#{version} and deleted=0")
    int deleteItem(@Param("basicId") String basicId, @Param("id") String id,
                   @Param("version") int version, @Param("actor") String actor);
}
