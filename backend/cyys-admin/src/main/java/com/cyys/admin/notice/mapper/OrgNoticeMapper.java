package com.cyys.admin.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.notice.model.OrgNotice;
import com.cyys.common.mybatis.ProtectedMapper;
import org.apache.ibatis.annotations.*;

@Mapper
@ProtectedMapper(OrgNotice.class)
public interface OrgNoticeMapper extends BaseMapper<OrgNotice> {
    @Select("select * from sys_org_notice where id=#{id} and deleted=0 for update")
    OrgNotice lock(String id);

    @Select("select * from sys_org_notice where id=#{id} and deleted=0 for share")
    OrgNotice forRead(String id);

    @Update("update sys_org_notice set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP where id=#{id} and version=#{version} and deleted=0")
    int remove(@Param("id") String id,@Param("version") int version,@Param("actor") String actor);

    @Select("<script>select id,title as title,scope_id,null as scope_name,org_id as department_id,null as department_name,updated_by as deleted_by,null as deleted_by_name,updated_at as deleted_at,created_at,version from sys_org_notice where deleted=1 "
            + "<if test='search!=null and search!=&quot;&quot;'>and title like concat('%',#{search},'%')</if> "
            + "order by created_at desc,id desc</script>")
    com.baomidou.mybatisplus.core.metadata.IPage<com.cyys.admin.recycle.dto.RecycleRecord> recycled(
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.cyys.admin.recycle.dto.RecycleRecord> page,
            @Param("search") String search);

    @Select("select * from sys_org_notice where id=#{id} and deleted=1 for update")
    OrgNotice lockRecycled(String id);

    @Update("update sys_org_notice set deleted=0,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=1")
    int restore(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);

    @Delete("delete from sys_org_notice where id=#{id} and version=#{version} and deleted=1")
    int purge(@Param("id") String id, @Param("version") int version);
}
