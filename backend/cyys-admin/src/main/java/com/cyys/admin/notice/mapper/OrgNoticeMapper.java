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

}
