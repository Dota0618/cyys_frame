package com.cyys.admin.dictionary.dto;

import com.cyys.admin.dictionary.model.ParamBasic;

public record DictionaryView(String id, String name, String systemName, int sort, int version) {
    public static DictionaryView from(ParamBasic row) {
        return new DictionaryView(row.getId(), row.getName(), row.getSystemName(), row.getSort(), row.getVersion());
    }
}
