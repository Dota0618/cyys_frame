package com.cyys.admin.auth.mapper;

import com.cyys.admin.user.model.SysUserScope;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;

/** 功能权限与机构数据范围独立读取，每次请求实时校验。 */
@Mapper
@ManualDataScope
public interface DataAuthorizationMapper {
    @Select("select code from sys_menu where status=1 and deleted=0 and type=3 order by code")
    java.util.List<String> activePermissionCodes();

    @Select("select distinct m.code " + com.cyys.admin.user.mapper.SysUserRoleMapper.VALID_GRANT
            + "join sys_role_menu rm on rm.role_id=r.id and rm.deleted=0 "
            + "join sys_menu m on m.id=rm.menu_id and m.status=1 and m.deleted=0 and m.type=3 "
            + com.cyys.admin.user.mapper.SysUserRoleMapper.USER_GRANT)
    java.util.List<String> permissions(@Param("userId") String actor);

    String AVAILABLE_INSTITUTIONS = """
            from sys_user_scope p
            join sys_user u on u.id=p.user_id and u.status=1 and u.deleted=0
            join sys_scope s on s.status=1 and s.deleted=0
            where p.user_id=#{actor} and p.status=1 and p.deleted=0
              and (p.data_range=0 or (exists(select 1 from sys_scope home
                   where home.id=p.scope_id and home.status=1 and home.deleted=0)
                   and (s.id=p.scope_id or (p.data_range=6 and find_in_set(p.scope_id,s.ancestors)>0))))
            """;

    @Select("""
            select exists(select 1 from sys_user_role ur
              join sys_user u on u.id=ur.user_id and u.status=1 and u.deleted=0
              join sys_role r on r.id=ur.role_id and r.status=1 and r.deleted=0
              join sys_role_menu rm on rm.role_id=r.id and rm.deleted=0
              join sys_menu m on m.id=rm.menu_id and m.status=1 and m.deleted=0 and m.type=3
              where ur.user_id=#{actor} and ur.deleted=0 and m.code=#{permission})
            """)
    boolean hasPermission(@Param("actor") String actor, @Param("permission") String permission);

    @Select("select exists(select 1 " + AVAILABLE_INSTITUTIONS + " and s.id=#{scope})")
    boolean hasInstitutionAccess(@Param("actor") String actor, @Param("scope") String scope);

    @Select("select exists(select 1 " + AVAILABLE_INSTITUTIONS + " and s.id=#{scope} and p.data_range in (0,1,6))")
    boolean hasInstitutionWideRange(@Param("actor") String actor, @Param("scope") String scope);

    @Select("select p.* from sys_user_scope p join sys_user u on u.id=p.user_id and u.status=1 and u.deleted=0 "
            + "where p.user_id=#{actor} and p.status=1 and p.deleted=0")
    SysUserScope activeProfile(String actor);

    @Select("select exists(select 1 from sys_user_scope p join sys_user u on u.id=p.user_id and u.status=1 and u.deleted=0 "
            + "where p.user_id=#{actor} and p.status=1 and p.deleted=0 and p.data_range=0)")
    boolean hasAllDataRange(String actor);
}
