package com.cyys.admin.role.mapper;

import com.cyys.admin.role.dto.RoleOption;
import com.cyys.common.mybatis.ManualDataScope;

import org.apache.ibatis.annotations.*;
import java.util.List;

/** 角色授权候选目录；目标用户的数据归属在服务中独立校验。 */
@Mapper
@ManualDataScope
public interface RoleGrantMapper {
    @Select("select * from sys_user_scope where user_id=#{user} and status=1 and deleted=0 for update")
    com.cyys.admin.user.model.SysUserScope lockProfile(String user);

    @Select("<script>select distinct rm.role_id as roleId,m.code from sys_role_menu rm "
            + "join sys_menu m on m.id=rm.menu_id and m.deleted=0 and m.status=1 and m.type=3 "
            + "where rm.deleted=0 and rm.role_id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<GrantPermission> permissions(@Param("ids") List<String> ids);

    @Select("select id as value,name as label from sys_role where status=1 and deleted=0 order by sort,id")
    List<RoleOption> activeRoles();

    record GrantPermission(String roleId, String code) {}
}
