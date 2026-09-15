package com.cyys.common.satoken;

import cn.dev33.satoken.stp.StpInterface;
import com.cyys.common.web.ApiException;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CyysSaTokenStpInterface implements StpInterface {
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return identityFor(loginId).permissions();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return identityFor(loginId).roles();
    }

    private RequestIdentity identityFor(Object loginId) {
        RequestIdentity identity = ScopeContext.get();
        if (!identity.loginId().equals(String.valueOf(loginId))) {
            throw new ApiException(403, "禁止复用其他账号的请求权限");
        }
        return identity;
    }
}
