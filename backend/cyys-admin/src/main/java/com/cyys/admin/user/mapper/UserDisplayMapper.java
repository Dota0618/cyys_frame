package com.cyys.admin.user.mapper;

import com.cyys.common.mybatis.ManualDataScope;

import com.cyys.admin.user.dto.UserLabel;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
@ManualDataScope
public interface UserDisplayMapper {
    @Select("<script>select u.id,coalesce(nullif(us.display_name,''),nullif(u.nickname,''),nullif(u.real_name,''),u.login_name) as name, "
            + "(u.status=1 and us.status=1) as active "
            + "from sys_user u join sys_user_scope us on us.user_id=u.id and us.deleted=0 "
            + "where u.deleted=0 and u.id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<UserLabel> labels(@Param("ids") List<String> ids);
}
