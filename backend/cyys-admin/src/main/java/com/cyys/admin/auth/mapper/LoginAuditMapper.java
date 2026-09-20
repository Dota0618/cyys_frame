package com.cyys.admin.auth.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cyys.admin.auth.dto.LoginAuditView;
import com.cyys.common.mybatis.ManualDataScope;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
@ManualDataScope
public interface LoginAuditMapper {
    @Insert("insert into sys_login_audit(id,login_name,user_id,scope_id,client_ip,outcome,reason) "
            + "values(#{id},#{loginName},#{userId},#{scopeId},#{clientIp},#{outcome},#{reason})")
    int insert(@Param("id") String id, @Param("loginName") String loginName,
               @Param("userId") String userId, @Param("scopeId") String scopeId,
               @Param("clientIp") String clientIp, @Param("outcome") String outcome,
               @Param("reason") String reason);

    @Select("<script>select id,login_name as loginName,user_id as userId,scope_id as scopeId,"
            + "client_ip as clientIp,outcome,reason,occurred_at as occurredAt "
            + "from sys_login_audit where scope_id=#{scope} "
            + "<if test='loginName != null and loginName != &quot;&quot;'>and login_name like concat('%',#{loginName},'%')</if> "
            + "<if test='outcome != null and outcome != &quot;&quot;'>and outcome=#{outcome}</if> "
            + "order by occurred_at desc,id desc</script>")
    IPage<LoginAuditView> page(Page<LoginAuditView> page, @Param("scope") String scope,
                                   @Param("loginName") String loginName, @Param("outcome") String outcome);
}
