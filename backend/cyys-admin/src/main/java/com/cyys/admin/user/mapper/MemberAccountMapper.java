package com.cyys.admin.user.mapper;

import com.cyys.admin.user.dto.MemberDTO.Option;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 用户管理专用控制语句；成员行授权后才能批量取得账号标识或当前单位关联。 */
@Mapper
public interface MemberAccountMapper {
    @Select("<script>select id as value,login_name as label from sys_user where deleted=0 and id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Option> accountNames(@Param("ids") List<String> ids);

    @Insert("insert into sys_user(id,login_name,password_hash,created_by,updated_by) values(#{id},#{login},#{hash},#{actor},#{actor})")
    int createAccount(@Param("id") String id,@Param("login") String login,@Param("hash") String hash,@Param("actor") String actor);

    @Select("select role_id from sys_user_role where user_id=#{user} and scope_id=#{scope} and deleted=0 order by created_at desc,id")
    List<String> roleIds(@Param("user") String user,@Param("scope") String scope);

}
