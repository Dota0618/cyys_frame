package com.cyys.admin.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.user.model.SysUserScope;
import com.cyys.common.mybatis.ProtectedMapper;
import org.apache.ibatis.annotations.*;

@Mapper
@ProtectedMapper(SysUserScope.class)
public interface MemberMapper extends BaseMapper<SysUserScope> {
    @Select("<script>select * from sys_user_scope where deleted=0 "
            + "<if test='name != null'>and display_name like concat('%',#{name},'%')</if> "
            + "order by created_at desc,id limit #{limit}</script>")
    @Options(timeout = 15)
    java.util.List<SysUserScope> export(@Param("name") String name, @Param("limit") int limit);

    @Select("select * from sys_user_scope where id=#{id} and deleted=0 for update")
    SysUserScope lockForEdit(String id);

    @Select("select * from sys_user_scope where id=#{id} and deleted=0 for update")
    SysUserScope lockForGrant(String id);

    @Select("select * from sys_user_scope where deleted=0 order by created_at desc,id")
    java.util.List<SysUserScope> acrossScopes(com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUserScope> page);
}
