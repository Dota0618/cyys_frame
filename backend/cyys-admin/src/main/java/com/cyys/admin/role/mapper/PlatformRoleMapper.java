package com.cyys.admin.role.mapper;

import com.cyys.common.mybatis.ManualDataScope;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.role.dto.PlatformRoleMenuOption;
import com.cyys.admin.role.dto.PlatformRolePermission;
import com.cyys.admin.role.dto.PlatformAccountRow;
import com.cyys.admin.role.model.SysRole;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
@ManualDataScope
public interface PlatformRoleMapper {
    @Select("<script>select * from sys_role where scope_id='GLOBAL' and deleted=0 "
            + "<if test='search != null and search != &quot;&quot;'>and (name like concat('%',#{search},'%') or code like concat('%',#{search},'%')) </if>"
            + "order by sort,id</script>")
    IPage<SysRole> page(Page<SysRole> page, @Param("search") String search);

    @Select("select * from sys_role where id=#{id} and scope_id='GLOBAL' and deleted=0")
    SysRole detail(@Param("id") String id);

    @Select("select * from sys_role where id=#{id} and scope_id='GLOBAL' and deleted=0 for update")
    SysRole lock(@Param("id") String id);

    @Select("select m.id,m.parent_id as parentId,m.name,m.code,m.type from sys_menu m "
            + "where m.status=1 and m.deleted=0 order by m.sort,m.id")
    List<PlatformRoleMenuOption> menus();

    @Select("select menu_id from sys_role_menu where role_id=#{role} and deleted=0 order by menu_id")
    List<String> menuIds(@Param("role") String role);

    @Select("<script>select m.id,m.parent_id as parentId,m.name,m.code,m.type from sys_menu m "
            + "where m.status=1 and m.deleted=0 and m.id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> for update</script>")
    List<PlatformRoleMenuOption> lockMenus(@Param("ids") List<String> ids);

    @Insert("insert into sys_role(id,scope_id,code,name,data_range,status,sort,remark,created_by,updated_by) "
            + "values(#{role.id},'GLOBAL',#{role.code},#{role.name},1,#{role.status},#{role.sort},#{role.remark},#{actor},#{actor})")
    int create(@Param("role") SysRole role, @Param("actor") String actor);

    @Update("update sys_role set name=#{role.name},status=#{role.status},sort=#{role.sort},remark=#{role.remark},"
            + "version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{role.id} and scope_id='GLOBAL' and version=#{role.version} and deleted=0")
    int updateRole(@Param("role") SysRole role, @Param("actor") String actor);

    @Update("update sys_role set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and scope_id='GLOBAL' and version=#{version} and deleted=0")
    int softDelete(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);

    @Select("select exists(select 1 from sys_user_role where role_id=#{role} and scope_id='GLOBAL' and deleted=0)")
    boolean hasUserGrant(@Param("role") String role);

    @Delete("delete from sys_role_menu where role_id=#{role}")
    int removeMenus(@Param("role") String role);

    @Insert("insert into sys_role_menu(id,role_id,menu_id,created_by,updated_by) values(#{id},#{role},#{menu},#{actor},#{actor})")
    int addMenu(@Param("id") String id, @Param("role") String role,
                @Param("menu") String menu, @Param("actor") String actor);

    @Select("<script>select distinct m.code,m.platform_only as platformOnly from sys_role_menu rm join sys_menu m on m.id=rm.menu_id "
            + "where rm.deleted=0 and m.deleted=0 and m.status=1 and m.type=3 and rm.role_id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<PlatformRolePermission> permissions(@Param("ids") List<String> ids);

    @Select("<script>select * from sys_role where scope_id='GLOBAL' and status=1 and deleted=0 and id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> for update</script>")
    List<SysRole> lockRoles(@Param("ids") List<String> ids);

    @Select("<script>select id,login_name as loginName,real_name as realName,status from sys_user where deleted=0 "
            + "<if test='search != null and search != &quot;&quot;'>and (login_name like concat('%',#{search},'%') or real_name like concat('%',#{search},'%')) </if>"
            + "order by created_at desc,id desc</script>")
    IPage<PlatformAccountRow> accounts(Page<PlatformAccountRow> page, @Param("search") String search);

    @Select("select id,login_name as loginName,real_name as realName,status from sys_user where id=#{id} and deleted=0 for update")
    PlatformAccountRow lockAccount(@Param("id") String id);

    @Select("select role_id from sys_user_role where user_id=#{user} and scope_id='GLOBAL' and deleted=0 order by role_id")
    List<String> accountRoleIds(@Param("user") String user);

    @Delete("delete from sys_user_role where user_id=#{user} and scope_id='GLOBAL'")
    int removeAccountRoles(@Param("user") String user);

    @Insert("insert into sys_user_role(id,user_id,role_id,scope_id,created_by,updated_by) "
            + "values(#{id},#{user},#{role},'GLOBAL',#{actor},#{actor})")
    int addAccountRole(@Param("id") String id, @Param("user") String user,
                       @Param("role") String role, @Param("actor") String actor);
}
