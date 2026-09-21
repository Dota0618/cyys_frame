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
        invalidate(group, java.util.List.of());
    }

    /** 共享账号名称变更只失效实际账号，不清除其他账号的名称。 */
    public void invalidateEntriesAfterCommit(CacheGroup group, java.util.List<String> ids) {
        if (ids.isEmpty()) throw new IllegalArgumentException("失效记录不能为空");
        if (ids.stream().anyMatch(id -> id == null || id.isBlank() || id.startsWith("@")))
            throw new IllegalArgumentException("非法缓存记录编号");
        invalidate(group, ids.stream().distinct().sorted().toList());
    }

    private void invalidate(CacheGroup group, java.util.List<String> fields) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive())
            throw new IllegalStateException("业务缓存失效必须在数据库事务内登记");
        var keys = cache.groupKeys(group);
        for (var sync : TransactionSynchronizationManager.getSynchronizations()) {
            if (sync instanceof Invalidation pending && pending.keys.equals(keys)) {
                if (pending.fields.isEmpty() || pending.fields.equals(fields)) return;
            }
        }
        TransactionSynchronizationManager.registerSynchronization(new Invalidation(keys, fields));
    }

    public void refresh(CacheGroup group) {
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("手动缓存刷新不能处于数据库事务中");
        cache.evict(cache.groupKeys(group), true);
    }

    private final class Invalidation implements TransactionSynchronization {
        private final java.util.List<String> keys;
        private final java.util.List<String> fields;
        private Invalidation(java.util.List<String> keys, java.util.List<String> fields) { this.keys = keys; this.fields = fields; }
        @Override public void afterCommit() { cache.evict(keys, fields, false); }
    }
}
