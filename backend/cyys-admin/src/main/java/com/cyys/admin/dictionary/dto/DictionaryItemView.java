package com.cyys.admin.dictionary.dto;

import com.cyys.admin.dictionary.model.ParamBasicType;

public record DictionaryItemView(String id, String basicId, String paramKey, String paramValue,
                                 String desp, int sort, int version) {
    public static DictionaryItemView from(ParamBasicType row) {
        return new DictionaryItemView(row.getId(), row.getBasicId(), row.getParamKey(), row.getParamValue(),
                row.getDesp(), row.getSort(), row.getVersion());
    }
}
