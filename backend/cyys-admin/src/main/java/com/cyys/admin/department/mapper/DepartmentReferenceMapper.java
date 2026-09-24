package com.cyys.admin.department.mapper;

import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 在调用方已锁定并验证部门后，仅检查服务端限定的关联关系。 */
@Mapper
@ManualDataScope
public interface DepartmentReferenceMapper {
    @Select("select exists(select 1 from sys_org where parent_id=#{id} and scope_id=#{scope} and deleted=0) "
            + "or exists(select 1 from sys_user_scope where org_id=#{id} and scope_id=#{scope} and deleted=0) "
            + "or exists(select 1 from sys_user_scope_org where org_id=#{id} and scope_id=#{scope}) "
            + "or exists(select 1 from sys_file_entry where org_id=#{id} and scope_id=#{scope} and deleted=0) "
            + "or exists(select 1 from sys_file_dir where org_id=#{id} and scope_id=#{scope} and deleted=0)")
    boolean hasActiveReferences(@Param("id") String id, @Param("scope") String scope);
}
