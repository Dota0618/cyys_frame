package com.cyys.common.satoken;

/** common 定义请求身份契约，admin 根据账号、成员和授权数据实现。 */
public interface IdentityResolver {
    RequestIdentity resolve(String loginId, String requestedScopeId, String credentialStamp);
}
