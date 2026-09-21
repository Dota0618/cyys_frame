package com.cyys.admin.user.mapper;

import com.cyys.common.mybatis.ManualDataScope;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.user.model.SysUserRole;
import com.cyys.admin.menu.dto.NavigationMenu;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 功能授权只有一套；机构选择和用户数据范围不参与角色、菜单计算。 */
@Mapper
@ManualDataScope
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
    String VALID_GRANT = """
            from sys_user_role ur
            join sys_user u on u.id=ur.user_id and u.status=1 and u.deleted=0
            join sys_user_scope p on p.user_id=ur.user_id and p.status=1 and p.deleted=0
            join sys_role r on r.id=ur.role_id and r.deleted=0 and r.status=1
            """;
    String USER_GRANT = "where ur.user_id=#{userId} and ur.deleted=0 ";

    @Select("select distinct r.code " + VALID_GRANT + USER_GRANT + "order by r.code")
    List<String> selectRoleCodes(String userId);

    @Select("select distinct m.code " + VALID_GRANT
            + "join sys_role_menu rm on rm.role_id=r.id and rm.deleted=0 "
            + "join sys_menu m on m.id=rm.menu_id and m.deleted=0 and m.status=1 and m.type=3 "
            + USER_GRANT + "order by m.code")
    List<String> selectPermissionCodes(String userId);

    @Select("select distinct m.id,m.parent_id,m.name,m.path,m.type,m.sort,m.icon " + VALID_GRANT
            + "join sys_role_menu rm on rm.role_id=r.id and rm.deleted=0 "
            + "join sys_menu m on m.id=rm.menu_id and m.deleted=0 and m.status=1 and m.visible=1 and m.type in (1,2) "
            + USER_GRANT + "order by m.sort,m.id")
    List<NavigationMenu> selectNavigation(String userId);
}
