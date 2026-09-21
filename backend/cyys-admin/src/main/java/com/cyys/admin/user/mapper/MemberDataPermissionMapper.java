package com.cyys.admin.user.mapper;

import com.cyys.admin.user.dto.MemberDepartmentOption;
import com.cyys.admin.user.model.SysUserScope;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
@ManualDataScope
public interface MemberDataPermissionMapper {
    @Select("select * from sys_user_scope where id=#{id} and (scope_id=#{scope} or scope_id is null) and deleted=0")
    SysUserScope detail(@Param("id") String id, @Param("scope") String scope);

    // 固定顺序锁住操作者及目标成员，禁止并发收回管理范围时继续扩大他人范围。
    @Select("select * from sys_user_scope where deleted=0 "
            + "and (id=#{id} or user_id=#{actor}) order by id for update")
    List<SysUserScope> lockMembers(@Param("id") String id, @Param("scope") String scope, @Param("actor") String actor);

    @Select("select org_id from sys_user_scope_org where member_id=#{id} and scope_id=#{scope} order by org_id")
    List<String> orgIds(@Param("id") String id, @Param("scope") String scope);

    @Select("select id as value,name as label,parent_id from sys_org where scope_id=#{scope} and deleted=0 and status=1 order by sort,id")
    List<MemberDepartmentOption> departments(String scope);

    @Select("<script>select id from sys_org where scope_id=#{scope} and status=1 and deleted=0 and id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> order by id for update</script>")
    List<String> lockDepartments(@Param("ids") List<String> ids, @Param("scope") String scope);

    @Update("update sys_user_scope set data_range=#{range},version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and scope_id <=> #{scope} and deleted=0 and version=#{version}")
    int update(@Param("id") String id, @Param("scope") String scope, @Param("version") int version,
               @Param("range") int range, @Param("actor") String actor);

    @Delete("delete from sys_user_scope_org where member_id=#{id} and scope_id=#{scope}")
    int removeDepartments(@Param("id") String id, @Param("scope") String scope);

    @Insert("insert into sys_user_scope_org(member_id,scope_id,org_id) values(#{id},#{scope},#{org})")
    int addDepartment(@Param("id") String id, @Param("scope") String scope, @Param("org") String org);
}
