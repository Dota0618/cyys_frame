package com.cyys.admin.notice.dto;

import com.cyys.common.cache.CacheValue;

import java.util.List;
import com.cyys.admin.department.dto.DepartmentOption;
import com.cyys.admin.dictionary.dto.DictionaryOption;

public record OrgNoticeOptions(List<DepartmentOption> departments,
        @CacheValue(cache="dictionary-options", variant="NOTICE_TYPE") List<DictionaryOption> types) {}
