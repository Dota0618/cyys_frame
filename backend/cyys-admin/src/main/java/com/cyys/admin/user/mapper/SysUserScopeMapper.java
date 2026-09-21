package com.cyys.admin.user.mapper;

import com.cyys.common.mybatis.ManualDataScope;
import com.cyys.admin.auth.mapper.DataAuthorizationMapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.auth.dto.LoginVO.ScopeInfo;
import com.cyys.admin.user.model.SysUserScope;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
@ManualDataScope
public interface SysUserScopeMapper extends BaseMapper<SysUserScope> {
    @Select("select s.id,s.code,s.name,coalesce(s.id=p.scope_id,0) as default_flag,s.parent_id "
            + DataAuthorizationMapper.AVAILABLE_INSTITUTIONS + " order by s.sort,s.id")
    List<ScopeInfo> selectAvailableScopes(@Param("actor") String actor);

    @Select("select * from sys_user_scope where user_id=#{userId} and status=1 and deleted=0")
    SysUserScope profile(String userId);
}
