package com.cyys.admin.scope.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.scope.model.SysScope;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
@ManualDataScope
public interface ScopeManagementMapper {
    @Select("select * from sys_scope where deleted=0 order by sort,id")
    List<SysScope> tree();

    // Serialize hierarchy edits, including insertion into an otherwise empty tree.
    @Select("select id from sys_menu where code='platform:scope:update' and deleted=0 for update")
    String lockStructure();

    @Select("select * from sys_scope where deleted=0 order by id for update")
    List<SysScope> lockTree();
    @Select("<script>select * from sys_scope where deleted=0 <if test='search!=null and search!=&quot;&quot;'>and (name like concat('%',#{search},'%') or code like concat('%',#{search},'%'))</if> order by sort,id</script>")
    IPage<SysScope> page(Page<SysScope> page,@Param("search") String search);

    @Select("select * from sys_scope where id=#{id} and deleted=0 for update")
    SysScope lock(@Param("id") String id);

    @Insert("insert into sys_scope(id,code,name,parent_id,ancestors,short_name,contact_person,contact_phone,remark,status,sort,created_by,updated_by) values(#{row.id},#{row.code},#{row.name},#{row.parentId},#{row.ancestors},#{row.shortName},#{row.contactPerson},#{row.contactPhone},#{row.remark},1,0,#{actor},#{actor})")
    int create(@Param("row") SysScope row,@Param("actor") String actor);

    @Update("update sys_user_scope set scope_id=#{root},version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP where scope_id is null and data_range=0 and deleted=0")
    int attachSystemAccounts(@Param("root") String root,@Param("actor") String actor);

    @Update("update sys_scope set name=#{row.name},parent_id=#{row.parentId},ancestors=#{row.ancestors},short_name=#{row.shortName},contact_person=#{row.contactPerson},contact_phone=#{row.contactPhone},remark=#{row.remark},status=#{row.status},sort=#{row.sort},version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP where id=#{row.id} and version=#{row.version} and deleted=0")
    int update(@Param("row") SysScope row,@Param("actor") String actor);

    @Update("update sys_scope set ancestors=#{ancestors},version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP where id=#{id} and deleted=0")
    int updateAncestors(@Param("id") String id,@Param("ancestors") String ancestors,@Param("actor") String actor);
}
