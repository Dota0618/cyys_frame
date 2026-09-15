package com.cyys.application.controller;

import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public HealthResponse health() {
        HealthResponse resp = new HealthResponse();
        resp.setStatus("UP");
        resp.setService("cyys-framework");
        resp.setTimestamp(LocalDateTime.now());
        resp.setVersion("1.0.0-SNAPSHOT");
        return resp;
    }

    @Data
    public static class HealthResponse {
        private String status;
        private String service;
        private String version;
        private LocalDateTime timestamp;
    }
}
