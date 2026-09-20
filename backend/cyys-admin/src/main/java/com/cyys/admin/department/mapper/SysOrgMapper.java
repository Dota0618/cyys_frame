package com.cyys.admin.department.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.department.model.SysOrg;
import com.cyys.common.mybatis.ProtectedMapper;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
@ProtectedMapper(SysOrg.class)
public interface SysOrgMapper extends BaseMapper<SysOrg> {
    @Select("select id as value,name as label,parent_id as parentId from sys_org "
            + "where status=1 and deleted=0 order by sort,id")
    List<com.cyys.admin.department.dto.DepartmentOption> treeOptionsForRead();

    @Select("select id as value,name as label,parent_id as parentId from sys_org "
            + "where status=1 and deleted=0 order by sort,id")
    List<com.cyys.admin.department.dto.DepartmentOption> parentOptionsForCreate();

    @Select("select * from sys_org where id=#{id} and deleted=0 and status=1 for update")
    SysOrg parentForCreate(@Param("id") String id);

    @Select("select * from sys_org where id=#{id} and deleted=0 and status=1 for share")
    SysOrg forReference(@Param("id") String id);

    @Select("select * from sys_org where id=#{id} and deleted=0 for update")
    SysOrg selectForUpdate(@Param("id") String id);

    @Select("select * from sys_org where id=#{id} and deleted=0")
    SysOrg selectForUpdateOptions(@Param("id") String id);

    @Select("<script>select * from sys_org where deleted=0 and id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> for update</script>")
    List<SysOrg> selectForDelete(@Param("ids") List<String> ids);

    @Update("update sys_org set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=0")
    int softDelete(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);

    @Select("select * from sys_org where deleted=0 order by sort,id")
    List<SysOrg> selectAcrossAuthorizedScopes();
}
