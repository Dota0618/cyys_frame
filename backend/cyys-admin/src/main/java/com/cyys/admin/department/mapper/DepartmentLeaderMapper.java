package com.cyys.admin.department.mapper;

import com.cyys.common.mybatis.ManualDataScope;

import com.cyys.admin.department.dto.DepartmentLeaderOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 部门负责人候选只暴露当前单位成员的选择标签，不开放成员管理数据。 */
@Mapper
@ManualDataScope
public interface DepartmentLeaderMapper {
    String LABEL = "coalesce(nullif(us.display_name,''),nullif(u.nickname,''),nullif(u.real_name,''),u.login_name)";
    String ACTIVE = "u.status=1 and u.deleted=0 and us.status=1 and us.deleted=0 "
            + "and (us.expire_at is null or us.expire_at>CURRENT_TIMESTAMP)";

    @Select("<script>select us.user_id as value," + LABEL + " as label,false as disabled "
            + "from sys_user_scope us join sys_user u on u.id=us.user_id "
            + "where us.scope_id=#{scope} and " + ACTIVE + " "
            + "<if test='search != null and search != \"\"'>and (" + LABEL
            + " like concat('%',#{search},'%') or u.login_name like concat('%',#{search},'%')) </if>"
            + "order by label,us.user_id limit 50</script>")
    List<DepartmentLeaderOption> activeOptions(@Param("scope") String scope,
                                                    @Param("search") String search);

    @Select("select us.user_id as value," + LABEL + " as label,false as disabled "
            + "from sys_user_scope us join sys_user u on u.id=us.user_id "
            + "where us.scope_id=#{scope} and us.user_id=#{userId} and " + ACTIVE + " for share")
    DepartmentLeaderOption eligibleForShare(@Param("scope") String scope,
                                                 @Param("userId") String userId);

    @Select("select us.user_id as value," + LABEL + " as label,not(" + ACTIVE + ") as disabled "
            + "from sys_user_scope us join sys_user u on u.id=us.user_id "
            + "where us.scope_id=#{scope} and us.user_id=#{userId} order by us.deleted,u.deleted limit 1")
    DepartmentLeaderOption historicalOption(@Param("scope") String scope,
                                                 @Param("userId") String userId);

    @Select("<script>select us.user_id as value," + LABEL + " as label,not(" + ACTIVE + ") as disabled "
            + "from sys_user_scope us join sys_user u on u.id=us.user_id "
            + "where us.scope_id=#{scope} and us.user_id in "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "order by us.user_id</script>")
    List<DepartmentLeaderOption> labels(@Param("scope") String scope,
                                            @Param("ids") List<String> ids);
}
