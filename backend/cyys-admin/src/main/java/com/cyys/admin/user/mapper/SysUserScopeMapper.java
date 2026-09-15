package com.cyys.admin.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.auth.dto.LoginVO.ScopeInfo;
import com.cyys.admin.user.model.SysUserScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SysUserScopeMapper extends BaseMapper<SysUserScope> {
    @Select("""
            select s.id, s.code, s.name, us.default_flag, us.member_role
            from sys_user_scope us
            join sys_scope s on s.id = us.scope_id and s.status = 1 and s.deleted = 0
            where us.user_id = #{userId} and us.status = 1 and us.deleted = 0
              and (us.expire_at is null or us.expire_at > CURRENT_TIMESTAMP)
            order by s.sort, s.id
            """)
    List<ScopeInfo> selectAvailableScopes(@Param("userId") String userId);
}
