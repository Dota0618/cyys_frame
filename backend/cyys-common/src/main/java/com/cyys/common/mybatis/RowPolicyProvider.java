package com.cyys.common.mybatis;

import com.cyys.common.satoken.RequestIdentity;

/** common 只持有 SQL 策略契约，账号、角色和部门数据规则由 admin 实现。 */
public interface RowPolicyProvider {
    void authorize(DataPolicyRegistry.Policy policy, RequestIdentity identity);
    String predicate(DataPolicyRegistry.Policy policy, RequestIdentity identity, String qualifier);
}
