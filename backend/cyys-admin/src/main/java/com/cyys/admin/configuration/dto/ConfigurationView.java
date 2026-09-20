package com.cyys.admin.configuration.dto;

import com.cyys.admin.configuration.model.Configure;
import com.fasterxml.jackson.annotation.JsonInclude;

public record ConfigurationView(String id, String name,
                                @JsonInclude(JsonInclude.Include.ALWAYS) String value, ConfigurationType type,
                                String desp, int sort, int version) {
    public static ConfigurationView from(Configure row) {
        return new ConfigurationView(row.getId(), row.getName(), row.getValue(),
                ConfigurationType.fromCode(row.getType()), row.getDesp(), row.getSort(), row.getVersion());
    }
}
