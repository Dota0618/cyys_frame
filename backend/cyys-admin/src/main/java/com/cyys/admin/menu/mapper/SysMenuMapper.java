package com.cyys.admin.menu.mapper;

import com.cyys.common.mybatis.ManualDataScope;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.menu.model.SysMenu;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
@ManualDataScope
public interface SysMenuMapper extends BaseMapper<SysMenu> {
    @Select("select * from sys_menu where deleted=0 order by sort,id")
    List<SysMenu> all();

    @Select("select * from sys_menu where id=#{id} and deleted=0")
    SysMenu detail(@Param("id") String id);

    @Select("select * from sys_menu where id=#{id} and deleted=0 for update")
    SysMenu lock(@Param("id") String id);

    @Select("select * from sys_menu where id=#{id} and deleted=0 and type<>3 for update")
    SysMenu lockParent(@Param("id") String id);

    @Select("""
            with recursive descendants as (
              select id from sys_menu where parent_id=#{root} and deleted=0
              union all
              select m.id from sys_menu m join descendants d on m.parent_id=d.id where m.deleted=0
            )
            select exists(select 1 from descendants where id=#{candidate})
            """)
    boolean isDescendant(@Param("root") String root, @Param("candidate") String candidate);

    @Select("select exists(select 1 from sys_menu where parent_id=#{id} and deleted=0)")
    boolean hasChildren(@Param("id") String id);

    @Select("select exists(select 1 from sys_role_menu where menu_id=#{id} and deleted=0)")
    boolean hasRoleReference(@Param("id") String id);

    @Insert("insert into sys_menu(id,parent_id,name,code,path,component,redirect,icon,type,sort,visible,status,"
            + "keep_alive,always_show,created_by,updated_by) values(#{menu.id},#{menu.parentId},"
            + "#{menu.name},#{menu.code},#{menu.path},#{menu.component},#{menu.redirect},#{menu.icon},#{menu.type},"
            + "#{menu.sort},#{menu.visible},#{menu.status},#{menu.keepAlive},#{menu.alwaysShow},#{actor},#{actor})")
    int create(@Param("menu") SysMenu menu, @Param("actor") String actor);

    @Update("update sys_menu set parent_id=#{menu.parentId},name=#{menu.name},path=#{menu.path},component=#{menu.component},"
            + "redirect=#{menu.redirect},icon=#{menu.icon},sort=#{menu.sort},visible=#{menu.visible},status=#{menu.status},"
            + "keep_alive=#{menu.keepAlive},always_show=#{menu.alwaysShow},"
            + "version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{menu.id} and version=#{menu.version} and deleted=0")
    int updateMenu(@Param("menu") SysMenu menu, @Param("actor") String actor);

    @Update("update sys_menu set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=0")
    int softDelete(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);
}
