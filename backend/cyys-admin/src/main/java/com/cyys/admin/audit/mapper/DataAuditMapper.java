package com.cyys.admin.audit.mapper;

import com.cyys.common.mybatis.ManualDataScope;
import com.cyys.admin.audit.dto.AuditView;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
@ManualDataScope
public interface DataAuditMapper {
    @Insert("insert into sys_data_audit(id,actor_id,scope_id,resource,record_id,operation,authorized_view,outcome) "
            + "values(#{id},#{actor},'GLOBAL','account',#{actor},'CHANGE_PASSWORD','ACCOUNT_SELF','SUCCESS')")
    int insertOwnPasswordChange(@Param("id") String id, @Param("actor") String actor);

    @Select("<script>select id,actor_id as actorId,scope_id as scopeId,resource,record_id as recordId,"
            + "operation,authorized_view as authorizedView,outcome,occurred_at as occurredAt "
            + "from sys_data_audit where scope_id=#{scope} "
            + "<if test='resource != null and resource != &quot;&quot;'>and resource=#{resource}</if> "
            + "<if test='operation != null and operation != &quot;&quot;'>and operation=#{operation}</if> "
            + "order by occurred_at desc,id desc</script>")
    IPage<AuditView> page(Page<AuditView> page, @Param("scope") String scope,
                              @Param("resource") String resource, @Param("operation") String operation);

    @Insert("insert into sys_data_audit(id,actor_id,scope_id,resource,record_id,operation,authorized_view,outcome) "
            + "values(#{id},#{actor},#{scope},#{resource},#{record},#{operation},'CURRENT_SCOPE','SUCCESS')")
    int insertSuccess(@Param("id") String id, @Param("actor") String actor, @Param("scope") String scope,
                      @Param("resource") String resource, @Param("record") String record, @Param("operation") String operation);

    @Insert("insert into sys_data_audit(id,actor_id,scope_id,resource,record_id,operation,authorized_view,outcome) "
            + "values(#{id},#{actor},'GLOBAL',#{resource},#{record},#{operation},'PLATFORM_CONTROL','SUCCESS')")
    int insertPlatformSuccess(@Param("id") String id, @Param("actor") String actor,
                              @Param("resource") String resource, @Param("record") String record,
                              @Param("operation") String operation);
}
