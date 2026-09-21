package com.cyys.admin.user.mapper;

import com.cyys.common.mybatis.ManualDataScope;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.user.model.SysUserRole;
import com.cyys.admin.menu.dto.NavigationMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
@ManualDataScope
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
    String VALID_GRANT = """
            from sys_user_role ur
            join sys_role r on r.id = ur.role_id and r.scope_id = ur.scope_id
                and r.deleted = 0 and r.status = 1
            """;
    String GRANT_SCOPE = """
            where ur.user_id = #{userId} and ur.deleted = 0
              and ur.scope_id in
              <foreach collection="scopeIds" item="scopeId" open="(" separator="," close=")">
                #{scopeId}
              </foreach>
              and (ur.scope_id = 'GLOBAL' or exists (
                select 1 from sys_user_scope us
                join sys_scope s on s.id = us.scope_id and s.status = 1 and s.deleted = 0
                where us.user_id = ur.user_id and us.scope_id = ur.scope_id
                  and us.status = 1 and us.deleted = 0
                  and (us.expire_at is null or us.expire_at &gt; CURRENT_TIMESTAMP)
              ))
            """;

    @Select("<script>select distinct r.code " + VALID_GRANT + GRANT_SCOPE + "</script>")
    List<String> selectRoleCodes(@Param("userId") String userId, @Param("scopeIds") List<String> scopeIds);

    @Select("""
            <script>select distinct m.code
            """ + VALID_GRANT + """
            join sys_role_menu rm on rm.role_id = r.id and rm.deleted = 0
            join sys_menu m on m.id = rm.menu_id and m.deleted = 0 and m.status = 1 and m.type = 3
            """ + GRANT_SCOPE + """
              and (r.scope_id = 'GLOBAL' or m.platform_only = 0)
            </script>
            """)
    List<String> selectPermissionCodes(@Param("userId") String userId, @Param("scopeIds") List<String> scopeIds);

    @Select("<script>select distinct m.id,m.parent_id,m.name,m.path,m.type,m.sort,m.icon " + VALID_GRANT
            + "join sys_role_menu rm on rm.role_id=r.id and rm.deleted=0 "
            + "join sys_menu m on m.id=rm.menu_id and m.deleted=0 and m.status=1 and m.visible=1 and m.type in (1,2) "
            + GRANT_SCOPE + " and (r.scope_id='GLOBAL' or m.platform_only=0) order by m.sort,m.id</script>")
    List<NavigationMenu> selectNavigation(@Param("userId") String userId,@Param("scopeIds") List<String> scopeIds);

    /** 授权管理要求操作者在当前单位的数据范围为全部；角色仅提供功能权限。 */
    @Select("""
            <script>select distinct m.code
            """ + VALID_GRANT + """
            join sys_role_menu rm on rm.role_id=r.id and rm.deleted=0
            join sys_menu m on m.id=rm.menu_id and m.deleted=0 and m.status=1 and m.type=3 and m.platform_only=0
            """ + GRANT_SCOPE + """
              and r.scope_id&lt;&gt;'GLOBAL' and exists(select 1 from sys_user_scope adm_us where adm_us.user_id=ur.user_id and adm_us.scope_id=ur.scope_id and adm_us.data_range=1 and adm_us.deleted=0 and adm_us.status=1 and (adm_us.expire_at is null or adm_us.expire_at &gt; CURRENT_TIMESTAMP))</script>
            """)
    List<String> selectUnitWidePermissionCodes(@Param("userId") String userId, @Param("scopeIds") List<String> scopeIds);
}
