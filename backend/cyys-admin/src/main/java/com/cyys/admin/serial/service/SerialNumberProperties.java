package com.cyys.admin.serial.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cyys.serial-numbers", ignoreUnknownFields = false)
public class SerialNumberProperties {
    private Map<String, SerialNumberDefinition> definitions = new LinkedHashMap<>();
}
