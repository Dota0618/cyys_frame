package com.cyys.admin.user.service;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 已授权业务内部的批量名称读取；缓存命中不额外查询单位、状态或权限。 */
@Service
@Validated
@RequiredArgsConstructor
public class UserDisplayService {
    private final UserCacheSource cache;

    public Map<String,String> names(@NotEmpty @Size(max=100) List<@NotBlank String> ids) {
        List<String> unique = ids.stream().distinct().toList();
        Map<String,String> result = new LinkedHashMap<>();
        cache.records(null, "", unique).forEach((id, row) -> result.put(id, row.name()));
        return Map.copyOf(result);
    }
}
