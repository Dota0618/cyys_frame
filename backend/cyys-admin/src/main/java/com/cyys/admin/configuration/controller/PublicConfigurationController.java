package com.cyys.admin.configuration.controller;

import com.cyys.admin.configuration.service.ConfigurationLookupService;
import com.cyys.common.web.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system/configurations")
public class PublicConfigurationController {
    private final ConfigurationLookupService service;

    @GetMapping
    public Object values() {
        return R.ok(service.publicValues());
    }
}
