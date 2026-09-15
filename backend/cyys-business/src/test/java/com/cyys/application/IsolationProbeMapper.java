package com.cyys.application;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.cyys.common.mybatis.DataAccess;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface IsolationProbeMapper {
    @Select("select d.id,d.scope_id from sys_org d where d.deleted=0 order by d.id")
    @DataAccess(resource="department", permission="department:read")
    List<Map<String,Object>> aliases();

    @Select("select d.id,d.scope_id,p.name as parent_name from sys_org d left join sys_org p on p.id=d.parent_id "
            + "where d.deleted=0 order by d.id")
    @DataAccess(resource="department", permission="department:read")
    List<Map<String,Object>> leftJoin();

    @Select("select d.id,d.scope_id from sys_org d where d.deleted=0 and d.id in (select s.id from sys_org s where s.deleted=0) order by d.id")
    @DataAccess(resource="department", permission="department:read")
    List<Map<String,Object>> subquery();

    @Select("select d.id,d.scope_id from sys_org d where d.deleted=0 and d.parent_id is null "
            + "union all select s.id,s.scope_id from sys_org s where s.deleted=0 and s.parent_id is not null")
    @DataAccess(resource="department", permission="department:read")
    List<Map<String,Object>> union();

    @Select("select scope_id,count(*) as total from sys_org where deleted=0 group by scope_id")
    @DataAccess(resource="department", permission="department:read")
    List<Map<String,Object>> groups();

    @Select("select count(*) from sys_org d where d.deleted=0")
    @DataAccess(resource="department", permission="department:read")
    long customCount();

    @Select("select d.id,(select count(*) from sys_org s where s.deleted=0) as total from sys_org d where d.deleted=0")
    @DataAccess(resource="department", permission="department:read")
    List<Map<String,Object>> scalarSubquery();

    @Select("select case when 1=1 then (select count(*) from sys_org s) else 0 end as total from sys_org d")
    @DataAccess(resource="department", permission="department:read")
    List<Map<String,Object>> uncoveredSubquery();

    @Select("select ((( from sys_org")
    @DataAccess(resource="department", permission="department:read")
    List<Map<String,Object>> malformed();

    @Select("select * from sys_scope")
    @DataAccess(resource="department", permission="department:read")
    List<Map<String,Object>> unregisteredTable();

    @Select("select * from sys_org")
    List<Map<String,Object>> unregisteredPolicy();

    @Select("select * from sys_org")
    @DataAccess(resource="unknown", permission="department:read")
    List<Map<String,Object>> unknownResource();

    @Select("select * from sys_org")
    @DataAccess(resource="department", permission="department:read")
    @Options(statementType=org.apache.ibatis.mapping.StatementType.STATEMENT)
    List<Map<String,Object>> rawStatement();

    @Select("select * from sys_org")
    @DataAccess(resource="department", permission="department:read")
    @InterceptorIgnore(dataPermission="true")
    List<Map<String,Object>> ignoredPolicy();

    @Update("update sys_org set name='unsafe' where scope_id=#{scope}")
    @DataAccess(resource="department", permission="department:update", operation=DataAccess.Operation.UPDATE)
    int unitWideUpdate(@Param("scope") String scope);

    @Update("update sys_org set scope_id=#{scope} where id=#{id} and version=0")
    @DataAccess(resource="department", permission="department:update", operation=DataAccess.Operation.UPDATE)
    int changeOwner(@Param("id") String id, @Param("scope") String scope);

    @Update("update sys_org set name='unsafe' where (id=#{id} and version=0) or 1=1")
    @DataAccess(resource="department", permission="department:update", operation=DataAccess.Operation.UPDATE)
    int disjunctiveWrite(@Param("id") String id);
}
