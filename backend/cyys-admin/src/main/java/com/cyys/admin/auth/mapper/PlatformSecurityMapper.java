package com.cyys.admin.auth.mapper;

import com.cyys.common.mybatis.ManualDataScope;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
@ManualDataScope
public interface PlatformSecurityMapper {
    @Select("select id from sys_menu where code='platform:scope:access' and deleted=0 for update")
    String lockAdministratorPermission();

    @Select("""
            select distinct u.id from sys_user u
            join sys_user_role ur on ur.user_id=u.id and ur.scope_id='GLOBAL' and ur.deleted=0
            join sys_role r on r.id=ur.role_id and r.scope_id='GLOBAL' and r.status=1 and r.deleted=0
            join sys_role_menu rm on rm.role_id=r.id and rm.deleted=0
            join sys_menu m on m.id=rm.menu_id and m.code='platform:scope:access'
                and m.status=1 and m.deleted=0 and m.type=3 and m.platform_only=1
            where u.status=1 and u.deleted=0 for update
            """)
    List<String> usableAdministrators();
}
