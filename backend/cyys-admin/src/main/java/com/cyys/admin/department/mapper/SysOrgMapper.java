package com.cyys.admin.department.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.department.model.SysOrg;
import com.cyys.common.mybatis.DataAccess;
import com.cyys.common.mybatis.ProtectedMapper;
import org.apache.ibatis.annotations.*;
import java.util.List;
import static com.cyys.common.mybatis.DataAccess.Operation.*;

@Mapper
@ProtectedMapper("department")
public interface SysOrgMapper extends BaseMapper<SysOrg> {
    @Select("select id as value,name as label,parent_id as parentId from sys_org "
            + "where status=1 and deleted=0 order by sort,id")
    @DataAccess(resource="department", permission="department:read")
    List<com.cyys.admin.department.dto.DepartmentDTO.Option> treeOptionsForRead();

    @Select("select id as value,name as label,parent_id as parentId from sys_org "
            + "where status=1 and deleted=0 order by sort,id")
    @DataAccess(resource="department", permission="department:create", operation=CREATE)
    List<com.cyys.admin.department.dto.DepartmentDTO.Option> parentOptionsForCreate();

    @Select("select * from sys_org where id=#{id} and deleted=0 and status=1 for update")
    @DataAccess(resource="department", permission="department:create", operation=CREATE)
    SysOrg parentForCreate(@Param("id") String id);

    @Select("select * from sys_org where id=#{id} and deleted=0 for update")
    @DataAccess(resource="department", permission="department:update", operation=UPDATE)
    SysOrg selectForUpdate(@Param("id") String id);

    @Select("<script>select * from sys_org where deleted=0 and id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> for update</script>")
    @DataAccess(resource="department", permission="department:delete", operation=DELETE)
    List<SysOrg> selectForDelete(@Param("ids") List<String> ids);

    @Update("update sys_org set deleted=1,version=version+1,updated_by=#{actor},updated_at=CURRENT_TIMESTAMP "
            + "where id=#{id} and version=#{version} and deleted=0")
    @DataAccess(resource="department", permission="department:delete", operation=DELETE)
    int softDelete(@Param("id") String id, @Param("version") int version, @Param("actor") String actor);

    /** 在已锁定且已按删除权限验证的部门上检查依赖，仅返回布尔值，不暴露隐藏关联。 */
    @Select("select exists(select 1 from sys_org where parent_id=#{id} and scope_id=#{scope} and deleted=0) "
            + "or exists(select 1 from sys_user_scope where org_id=#{id} and scope_id=#{scope} and deleted=0) "
            + "or exists(select 1 from sys_role_org where org_id=#{id} and scope_id=#{scope})")
    boolean hasActiveReferences(@Param("id") String id, @Param("scope") String scope);

    @Select("select * from sys_org where deleted=0 order by sort,id")
    @DataAccess(resource="department", permission="department:read", view=DataAccess.View.AUTHORIZED_SCOPES)
    List<SysOrg> selectAcrossAuthorizedScopes();
}
