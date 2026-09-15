package com.cyys.admin.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 仅返回指定账号、单位及操作的存在性，避免行策略计算递归调用自己。 */
@Mapper
public interface DataAuthorizationMapper {
    @Select("""
            select exists (
              select 1 from sys_user_role ur
              join sys_user u on u.id=ur.user_id and u.status=1 and u.deleted=0
              join sys_role r on r.id=ur.role_id and r.scope_id=ur.scope_id and r.status=1 and r.deleted=0
              join sys_role_menu rm on rm.role_id=r.id and rm.deleted=0
              join sys_menu m on m.id=rm.menu_id and m.status=1 and m.deleted=0 and m.type=3
              where ur.user_id=#{actor} and ur.scope_id=#{scope} and ur.deleted=0 and m.code=#{permission}
                and (ur.scope_id='GLOBAL' or (m.platform_only=0 and exists (
                  select 1 from sys_user_scope us join sys_scope s on s.id=us.scope_id and s.status=1 and s.deleted=0
                  where us.user_id=ur.user_id and us.scope_id=ur.scope_id and us.status=1 and us.deleted=0
                    and (us.expire_at is null or us.expire_at>CURRENT_TIMESTAMP)
                )))
            )
            """)
    boolean hasPermission(@Param("actor") String actor, @Param("scope") String scope, @Param("permission") String permission);
}
