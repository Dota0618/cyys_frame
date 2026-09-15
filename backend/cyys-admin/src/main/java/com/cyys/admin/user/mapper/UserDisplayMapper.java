package com.cyys.admin.user.mapper;

import com.cyys.admin.user.dto.UserLabel;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserDisplayMapper {
    @Select("<script>select us.user_id from sys_user_scope us "
            + "join sys_user u on u.id=us.user_id and u.status=1 and u.deleted=0 "
            + "join sys_scope s on s.id=us.scope_id and s.status=1 and s.deleted=0 "
            + "where us.scope_id=#{scope} and us.status=1 and us.deleted=0 "
            + "and (us.expire_at is null or us.expire_at>CURRENT_TIMESTAMP) and us.user_id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<String> visibleMembers(@Param("scope") String scope, @Param("ids") List<String> ids);

    @Select("<script>select u.id,coalesce(nullif(us.display_name,''),nullif(u.nickname,''),nullif(u.real_name,''),u.login_name) as name "
            + "from sys_user u join sys_user_scope us on us.user_id=u.id and us.scope_id=#{scope} and us.deleted=0 and us.status=1 "
            + "where u.deleted=0 and u.status=1 and u.id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<UserLabel> labels(@Param("scope") String scope, @Param("ids") List<String> ids);
}
