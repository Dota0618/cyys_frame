package com.cyys.admin.serial.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.auth.service.RoleRowPolicyProvider;
import com.cyys.admin.serial.mapper.SerialNumberMapper;
import com.cyys.common.mybatis.DataPermissionContext;
import com.cyys.common.mybatis.DataPolicyRegistry;
import com.cyys.common.mybatis.DataTransactionGuard;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

@Service
public class SerialNumberService {
    private final Map<String, SerialNumberDefinition> definitions;
    private final SerialNumberMapper numbers;
    private final RoleRowPolicyProvider authorization;
    private final DataAuditService audit;

    public SerialNumberService(SerialNumberProperties properties, SerialNumberMapper numbers,
                               RoleRowPolicyProvider authorization, DataAuditService audit) {
        definitions = Map.copyOf(properties.getDefinitions());
        var prefixes = new HashSet<String>();
        definitions.forEach((code, rule) -> {
            if (!code.matches("[a-z][a-z0-9-]{0,31}") || !prefixes.add(rule.prefix())) {
                throw new IllegalArgumentException("流水号业务编码无效或前缀重复");
            }
        });
        this.numbers = numbers;
        this.authorization = authorization;
        this.audit = audit;
    }

    /** 业务 Service 在授权后的写用例中调用；预留独立提交，业务回滚允许断号但不回收已预留号码。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED, timeout = 15)
    public String next(String businessCode) {
        var rule = businessCode == null ? null : definitions.get(businessCode);
        if (rule == null) throw new ApiException(409, "流水号业务规则未配置");
        var identity = ScopeContext.get();
        var operation = DataPermissionContext.get();
        if (operation.view() != DataPolicyRegistry.View.CURRENT_SCOPE)
            throw new ApiException(403, "跨机构查询不能申请流水号");
        String scope = identity.requireScopeId();
        if ("GLOBAL".equals(scope)) throw new ApiException(403, "流水号必须归属真实单位");
        DataTransactionGuard.bind(identity, DataPolicyRegistry.View.CURRENT_SCOPE);
        authorization.authorizeCurrentScope(rule.permission(), identity);
        if (!scope.equals(numbers.lockActiveScope(scope))) throw new ApiException(403, "流水号所属单位不可用");
        numbers.ensureRule(scope, businessCode, rule, identity.loginId());
        var saved = numbers.lockRule(scope, businessCode);
        if (saved == null || !saved.prefix().equals(rule.prefix()) || !saved.periodType().equals(rule.period().name())
                || saved.width() != rule.width() || !saved.zoneId().equals(rule.zone()) || !saved.permission().equals(rule.permission())) {
            throw new ApiException(409, "流水号规则已使用或前缀已占用，不能直接修改历史规则");
        }
        // 获得规则锁后计算周期，排队跨日的请求使用真正开始分配时的日期。
        var today = LocalDate.now(ZoneId.of(rule.zone()));
        String period = switch (rule.period()) {
            case NONE -> "ALL";
            case DAY -> today.format(DateTimeFormatter.BASIC_ISO_DATE);
            case MONTH -> today.format(DateTimeFormatter.ofPattern("uuuuMM", Locale.ROOT));
            case YEAR -> today.format(DateTimeFormatter.ofPattern("uuuu", Locale.ROOT));
        };
        numbers.ensureCounter(IdWorker.getIdStr(), scope, businessCode, period, identity.loginId());
        var counter = numbers.lockCounter(scope, businessCode, period);
        if (counter == null) throw new ApiException(409, "流水号初始化冲突，请重试");
        long maximum = Long.parseLong("9".repeat(rule.width()));
        if (counter.sequenceValue() >= maximum) throw new ApiException(409, "当前周期流水号已耗尽");
        long next = counter.sequenceValue() + 1;
        authorization.authorizeCurrentScope(rule.permission(), identity);
        if (numbers.advance(counter.id(), scope, businessCode, period, counter.sequenceValue(), next, identity.loginId()) != 1) {
            throw new ApiException(409, "流水号状态已变化，请重试");
        }
        audit.success("serial", counter.id(), "RESERVE");
        return rule.prefix() + (rule.period() == SerialPeriod.NONE ? "" : period + "-")
                + String.format(Locale.ROOT, "%0" + rule.width() + "d", next);
    }
}
