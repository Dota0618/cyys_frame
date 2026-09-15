package com.cyys.admin.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.role.dto.RoleDTO;
import com.cyys.admin.role.model.SysRole;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    @Select("<script>select * from sys_role where scope_id=#{scope} and deleted=0 "
            + "<if test='search != null and search != &quot;&quot;'>and (name like concat('%',#{search},'%') or code like concat('%',#{search},'%')) </if>"
            + "order by sort,id</script>")
    IPage<SysRole> page(Page<SysRole> page, @Param("scope") String scope, @Param("search") String search);

    @Select("select * from sys_role where id=#{id} and scope_id=#{scope} and deleted=0")
    SysRole detail(@Param("id") String id, @Param("scope") String scope);

    @Select("select * from sys_role where id=#{id} and scope_id=#{scope} and deleted=0 for update")
    SysRole lock(@Param("id") String id, @Param("scope") String scope);

    @Select("select m.id,m.parent_id as parentId,m.name,m.code,m.type from sys_menu m "
            + "where m.status=1 and m.deleted=0 and m.platform_only=0 order by m.sort,m.id")
    List<RoleDTO.MenuOption> menus();

    @Select("select o.id,o.parent_id as parentId,o.name from sys_org o "
            + "where o.scope_id=#{scope} and o.status=1 and o.deleted=0 order by o.sort,o.id")
    List<RoleDTO.DepartmentOption> departments(@Param("scope") String scope);

    @Select("select menu_id from sys_role_menu where role_id=#{role} and deleted=0 order by menu_id")
    List<String> menuIds(@Param("role") String role);

    @Select("select org_id from sys_role_org where role_id=#{role} and scope_id=#{scope} order by org_id")
    List<String> orgIds(@Param("role") String role, @Param("scope") String scope);

    @Select("<script>select m.id,m.parent_id as parentId,m.name,m.code,m.type from sys_menu m "
            + "where m.status=1 and m.deleted=0 and m.platform_only=0 and m.id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> for update</script>")
    List<RoleDTO.MenuOption> lockMenus(@Param("ids") List<String> ids);

    @Select("<script>select id from sys_org where scope_id=#{scope} and status=1 and deleted=0 and id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> for update</script>")
    List<String> lockDepartments(@Param("ids") List<String> ids, @Param("scope") String scope);

    @Insert("insert into sys_role(id,scope_id,code,name,data_range,status,sort,remark,created_by,updated_by) "
            + "values(#{role.id},#{role.scopeId},#{role.code},#{role.name},#{role.dataRange},#{role.status},#{role.sort},#{role.remark},#{actor},#{actor})")
    int create(@Param("role") SysRole role, @Param("actor") String actor);

    @Update("update sys_role set name=#{role.name},data_range=#{role.dataRange},status=#{role.status},sort=#{role.sort},"
            + "remark=#{role.remark},version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{role.id} and scope_id=#{role.scopeId} and version=#{role.version} and deleted=0")
    int updateRole(@Param("role") SysRole role, @Param("actor") String actor);

    @Update("update sys_role set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and scope_id=#{scope} and version=#{version} and deleted=0")
    int softDelete(@Param("id") String id, @Param("scope") String scope,
                   @Param("version") int version, @Param("actor") String actor);

    @Select("select exists(select 1 from sys_user_role where role_id=#{role} and scope_id=#{scope} and deleted=0)")
    boolean hasUserGrant(@Param("role") String role, @Param("scope") String scope);

    @Delete("delete from sys_role_menu where role_id=#{role}")
    int removeMenus(@Param("role") String role);

    @Insert("insert into sys_role_menu(id,role_id,menu_id,created_by,updated_by) values(#{id},#{role},#{menu},#{actor},#{actor})")
    int addMenu(@Param("id") String id, @Param("role") String role,
                @Param("menu") String menu, @Param("actor") String actor);

    @Delete("delete from sys_role_org where role_id=#{role} and scope_id=#{scope}")
    int removeDepartments(@Param("role") String role, @Param("scope") String scope);

    @Insert("insert into sys_role_org(role_id,scope_id,org_id) values(#{role},#{scope},#{org})")
    int addDepartment(@Param("role") String role, @Param("scope") String scope, @Param("org") String org);
}
