package com.cyys.admin.user.service;

import com.cyys.admin.cache.service.BusinessCacheGroups;
import com.cyys.admin.user.dto.UserLabel;
import com.cyys.admin.user.mapper.UserDisplayMapper;
import com.cyys.common.cache.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 仅供已授权响应中的实际引用；不是任意 ID 查询接口，也不提供候选资格。 */
@Component
@RequiredArgsConstructor
public class UserCacheSource implements CacheValueSource<UserLabel> {
    private final RecordCache cache;
    private final UserDisplayMapper mapper;
    public String name() { return "user"; }
    public CacheScope scope() { return CacheScope.SYSTEM; }
    public Class<UserLabel> recordType() { return UserLabel.class; }
    public Map<String,UserLabel> records(String scopeId, String variant, List<String> ids) {
        if (!variant.isEmpty()) return Map.of();
        return cache.getOrLoad(BusinessCacheGroups.USER_NAMES, null, ids, UserLabel.class,
                missing -> mapper.labels(missing).stream().collect(Collectors.toMap(UserLabel::id, Function.identity())));
    }
}
