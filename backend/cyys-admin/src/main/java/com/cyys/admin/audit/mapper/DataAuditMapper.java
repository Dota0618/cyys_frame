package com.cyys.admin.audit.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DataAuditMapper {
    @Insert("insert into sys_data_audit(id,actor_id,scope_id,resource,record_id,operation,authorized_view,outcome) "
            + "values(#{id},#{actor},#{scope},#{resource},#{record},#{operation},'CURRENT_SCOPE','SUCCESS')")
    int insertSuccess(@Param("id") String id, @Param("actor") String actor, @Param("scope") String scope,
                      @Param("resource") String resource, @Param("record") String record, @Param("operation") String operation);
}
