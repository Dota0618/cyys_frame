package com.cyys.admin.notice.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.notice.model.SystemNotice;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;

/** 全局内容：入口必须经 PlatformSecurityService 核验 GLOBAL 操作权限。 */
@Mapper
@ManualDataScope
public interface SystemNoticeMapper {
    String COLUMNS = "id,title,type,type_label,content,is_show as visible,version,created_at";

    @Select("<script>select " + COLUMNS + " from sys_notice where deleted=0 "
            + "<if test='title != null and title != &quot;&quot;'>and title like concat('%',#{title},'%')</if> "
            + "order by created_at desc,id desc</script>")
    IPage<SystemNotice> page(Page<SystemNotice> page, @Param("title") String title);

    @Select("select " + COLUMNS + " from sys_notice where id=#{id} and deleted=0")
    SystemNotice detail(String id);

    @Insert("insert into sys_notice(id,title,type,type_label,content,is_show,created_by,updated_by) "
            + "values(#{row.id},#{row.title},#{row.type},#{row.typeLabel},#{row.content},#{row.visible},#{actor},#{actor})")
    int create(@Param("row") SystemNotice row, @Param("actor") String actor);

    @Update("update sys_notice set title=#{row.title},type=#{row.type},type_label=#{row.typeLabel},"
            + "content=#{row.content},is_show=#{row.visible},version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{row.id} and version=#{row.version} and deleted=0")
    int update(@Param("row") SystemNotice row, @Param("actor") String actor);

    @Update("update sys_notice set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=0")
    int softDelete(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);
}
