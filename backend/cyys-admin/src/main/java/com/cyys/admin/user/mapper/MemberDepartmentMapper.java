package com.cyys.admin.user.mapper;

import com.cyys.admin.user.dto.MemberDTO.DepartmentOption;
import com.cyys.common.mybatis.DataAccess;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MemberDepartmentMapper {
    @Select("select id as value,name as label,parent_id as parentId from sys_org where status=1 and deleted=0 order by sort,id")
    @DataAccess(resource="department",permission="user:read")
    List<DepartmentOption> options();

    @Select("select id from sys_org where id=#{id} and status=1 and deleted=0 for update")
    @DataAccess(resource="department",permission="user:create",operation=DataAccess.Operation.CREATE)
    String forCreate(String id);

    @Select("select id from sys_org where id=#{id} and status=1 and deleted=0 for update")
    @DataAccess(resource="department",permission="user:update",operation=DataAccess.Operation.UPDATE)
    String forEdit(String id);
}
