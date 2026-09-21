package @@PACKAGE@@.dto;

import com.cyys.admin.department.dto.DepartmentOption;
import com.cyys.admin.dictionary.dto.DictionaryOption;
import java.util.*;

public record @@CLASS@@Options(List<DepartmentOption> departments,List<@@CLASS@@View> parents,Map<String,List<DictionaryOption>> dictionaries) {}
