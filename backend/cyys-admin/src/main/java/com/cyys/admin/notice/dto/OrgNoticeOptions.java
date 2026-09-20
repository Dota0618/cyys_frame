package com.cyys.admin.notice.dto;

import java.util.List;
import com.cyys.admin.department.dto.DepartmentOption;
import com.cyys.admin.dictionary.dto.DictionaryOption;

public record OrgNoticeOptions(List<DepartmentOption> departments,List<DictionaryOption> types) {}
