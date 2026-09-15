package com.cyys.common.mybatis;

import com.cyys.common.satoken.RequestIdentity;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public final class DataTransactionGuard {
    private DataTransactionGuard() {}

    public static void bind(RequestIdentity identity, DataAccess.View view) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) return;
        var current = new TransactionIdentity(identity.loginId(), identity.scopeId(), view);
        Object existing = TransactionSynchronizationManager.getResource(DataTransactionGuard.class);
        if (existing != null && !existing.equals(current)) {
            throw DataPolicyRegistry.denied("同一事务不得切换账号、单位或授权视图");
        }
        if (existing == null) {
            TransactionSynchronizationManager.bindResource(DataTransactionGuard.class, current);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void suspend() { TransactionSynchronizationManager.unbindResource(DataTransactionGuard.class); }
                @Override public void resume() { TransactionSynchronizationManager.bindResource(DataTransactionGuard.class, current); }
                @Override public void afterCompletion(int status) { TransactionSynchronizationManager.unbindResourceIfPossible(DataTransactionGuard.class); }
            });
        }
    }

    private record TransactionIdentity(String actor, String scope, DataAccess.View view) {}
}
