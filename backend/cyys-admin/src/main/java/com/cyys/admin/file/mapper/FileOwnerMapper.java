package com.cyys.admin.file.mapper;

import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;

/** 只解析已验证操作者在当前单位的有效部门，不接受客户端指定归属。 */
@Mapper
@ManualDataScope
public interface FileOwnerMapper {
    @Select("select us.org_id from sys_user_scope us join sys_org org on org.id=us.org_id and org.scope_id=us.scope_id "
            + "and org.status=1 and org.deleted=0 where us.user_id=#{actor} and us.scope_id=#{scope} "
            + "and us.status=1 and us.deleted=0  for update")
    String department(@Param("actor") String actor, @Param("scope") String scope);
}
