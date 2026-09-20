package com.cyys.admin.notice.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.notice.dto.OrgNoticeReadView;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 先授权父通知；只查询父记录实际单位和已授权 ID，阅读人始终取当前真实身份。 */
@Mapper
@ManualDataScope
public interface OrgNoticeReadMapper {
    record Count(String id,long total,boolean seen) {}
    record DepartmentName(String id,String name) {}

    @Insert("insert into sys_org_notice_ref(scope_id,notice_id,user_id) values(#{scope},#{notice},#{actor}) on duplicate key update user_id=sys_org_notice_ref.user_id")
    int read(@Param("scope") String scope,@Param("notice") String notice,@Param("actor") String actor);

    @Select("<script>select notice_id as id,count(*) as total,max(user_id=#{actor}) as seen from sys_org_notice_ref where scope_id=#{scope} and notice_id in <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> group by notice_id</script>")
    List<Count> counts(@Param("scope") String scope,@Param("ids") List<String> ids,@Param("actor") String actor);

    @Select("<script>select id,name from sys_org where scope_id=#{scope} and id in <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<DepartmentName> departments(@Param("scope") String scope,@Param("ids") List<String> ids);

    @Select("select r.user_id,coalesce(nullif(us.display_name,''),nullif(u.nickname,''),nullif(u.real_name,''),u.login_name,r.user_id) as user_name,r.first_read_at " +
            "from sys_org_notice_ref r left join sys_user_scope us on us.scope_id=r.scope_id and us.user_id=r.user_id " +
            "left join sys_user u on u.id=r.user_id where r.scope_id=#{scope} and r.notice_id=#{notice} order by r.first_read_at desc,r.user_id")
    IPage<OrgNoticeReadView> page(Page<OrgNoticeReadView> page,@Param("scope") String scope,@Param("notice") String notice);
}
