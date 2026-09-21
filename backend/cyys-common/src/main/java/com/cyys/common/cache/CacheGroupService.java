package com.cyys.common.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 自动失效和手动刷新共用组定义；提交回调捕获固定键，不重新读取请求上下文。 */
@Component
@RequiredArgsConstructor
public class CacheGroupService {
    private final ScopedHashCache cache;

    public void invalidateAfterCommit(CacheGroup group) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive())
            throw new IllegalStateException("业务缓存失效必须在数据库事务内登记");
        var keys = cache.groupKeys(group);
        for (var sync : TransactionSynchronizationManager.getSynchronizations()) {
            if (sync instanceof Invalidation pending && pending.keys.equals(keys)) return;
        }
        TransactionSynchronizationManager.registerSynchronization(new Invalidation(keys));
    }

    public void refresh(CacheGroup group) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("手动缓存刷新不能处于数据库事务中");
        cache.evict(cache.groupKeys(group), true);
    }

    private final class Invalidation implements TransactionSynchronization {
        private final java.util.List<String> keys;
        private Invalidation(java.util.List<String> keys) { this.keys = keys; }
        @Override public void afterCommit() { cache.evict(keys, false); }
    }
}
