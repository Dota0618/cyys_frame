package com.cyys.admin.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.user.model.SysUserScope;
import com.cyys.common.mybatis.DataAccess;
import com.cyys.common.mybatis.ProtectedMapper;
import org.apache.ibatis.annotations.*;

@Mapper
@ProtectedMapper("user")
public interface MemberMapper extends BaseMapper<SysUserScope> {
    @Select("select * from sys_user_scope where id=#{id} and deleted=0 for update")
    @DataAccess(resource="user",permission="user:update",operation=DataAccess.Operation.UPDATE)
    SysUserScope lockForEdit(String id);

    @Select("select * from sys_user_scope where id=#{id} and deleted=0 for update")
    @DataAccess(resource="user",permission="role:grant",operation=DataAccess.Operation.UPDATE)
    SysUserScope lockForGrant(String id);

    @Select("select * from sys_user_scope where deleted=0 order by created_at desc,id")
    @DataAccess(resource="user",permission="user:read",view=DataAccess.View.AUTHORIZED_SCOPES)
    java.util.List<SysUserScope> acrossScopes(com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUserScope> page);
}
