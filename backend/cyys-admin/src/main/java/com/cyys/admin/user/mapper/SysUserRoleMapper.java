package com.cyys.admin.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.user.model.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
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

    /** 角色授予只使用本单位全部数据范围的权限作为上限，暂不开放局部范围委派。 */
    @Select("""
            <script>select distinct m.code
            """ + VALID_GRANT + """
            join sys_role_menu rm on rm.role_id=r.id and rm.deleted=0
            join sys_menu m on m.id=rm.menu_id and m.deleted=0 and m.status=1 and m.type=3 and m.platform_only=0
            """ + GRANT_SCOPE + """
              and r.scope_id&lt;&gt;'GLOBAL' and r.data_range=1</script>
            """)
    List<String> selectUnitWidePermissionCodes(@Param("userId") String userId, @Param("scopeIds") List<String> scopeIds);
}
