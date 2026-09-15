package com.cyys.admin.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyys.admin.user.model.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    /** 锁定账号行，使同一账号的并发密码尝试按事务串行计数。 */
    @Select("""
            select * from sys_user
            where login_name = #{loginName} and deleted = 0
            for update
            """)
    SysUser selectForLogin(@Param("loginName") String loginName);

    @Update("""
            update sys_user set pwd_error_count = #{count}, pwd_lock_time = #{lockedUntil},
                updated_by = id, updated_at = CURRENT_TIMESTAMP
            where id = #{id} and deleted = 0
            """)
    int recordPasswordFailure(@Param("id") String id, @Param("count") int count,
                              @Param("lockedUntil") LocalDateTime lockedUntil);

    @Update("""
            update sys_user set pwd_error_count = 0, pwd_lock_time = null,
                last_login_time = #{now}, last_login_ip = #{ip},
                updated_by = id, updated_at = #{now}
            where id = #{id} and deleted = 0 and status = 1
            """)
    int recordLoginSuccess(@Param("id") String id, @Param("now") LocalDateTime now,
                           @Param("ip") String ip);
}
