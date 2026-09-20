package com.cyys.admin.user.mapper;

import com.cyys.admin.user.dto.MemberDepartmentOption;
import com.cyys.admin.department.model.SysOrg;
import com.cyys.common.mybatis.ProtectedMapper;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
@ProtectedMapper(SysOrg.class)
public interface MemberDepartmentMapper {
    @Select("select id as value,name as label,parent_id as parentId from sys_org where status=1 and deleted=0 order by sort,id")
    List<MemberDepartmentOption> options();

    @Select("<script>select id as value,name as label,parent_id as parentId from sys_org where status=1 and deleted=0 and id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<MemberDepartmentOption> exportOptions(@Param("ids") List<String> ids);

    @Select("select id from sys_org where id=#{id} and status=1 and deleted=0 for update")
    String forCreate(String id);

    @Select("select id from sys_org where id=#{id} and status=1 and deleted=0 for update")
    String forEdit(String id);
}
