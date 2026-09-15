package com.cyys.admin.role.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;

/** 角色授予专用控制路径：不对外提供任意表操作，每次替换固定账号和单位。 */
@Mapper
public interface RoleGrantMapper {
    @Select("select id from sys_user_scope where user_id=#{user} and scope_id=#{scope} and status=1 and deleted=0 "
            + "and (expire_at is null or expire_at>CURRENT_TIMESTAMP) for update")
    String lockMembership(@Param("user") String user, @Param("scope") String scope);

    @Select("<script>select id from sys_role where scope_id=#{scope} and status=1 and deleted=0 and id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> for update</script>")
    List<String> lockUnitRoles(@Param("ids") List<String> ids, @Param("scope") String scope);

    @Select("<script>select distinct rm.role_id as roleId,m.code,m.platform_only from sys_role_menu rm "
            + "join sys_menu m on m.id=rm.menu_id and m.deleted=0 and m.status=1 and m.type=3 "
            + "where rm.deleted=0 and rm.role_id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<GrantPermission> permissions(@Param("ids") List<String> ids);

    @Select("select id as value,name as label from sys_role where scope_id=#{scope} and status=1 and deleted=0 order by sort,id")
    List<RoleOption> activeUnitRoles(@Param("scope") String scope);

    @Delete("delete from sys_user_role where user_id=#{user} and scope_id=#{scope}")
    int removeUnitGrants(@Param("user") String user, @Param("scope") String scope);

    @Insert("insert into sys_user_role(id,user_id,role_id,scope_id,created_by,updated_by) "
            + "values(#{id},#{user},#{role},#{scope},#{actor},#{actor})")
    int addUnitGrant(@Param("id") String id, @Param("user") String user, @Param("role") String role,
                     @Param("scope") String scope, @Param("actor") String actor);

    record GrantPermission(String roleId, String code, boolean platformOnly) {}
    record RoleOption(String value, String label) {}
}
