package com.cyys.admin.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(prefix="cyys.file-cleanup", name="enabled", havingValue="true", matchIfMissing=true)
public class FileCleanupScheduler {
    private final FileCleanupService cleanup;

    @Scheduled(fixedDelayString="${cyys.file-cleanup.interval-ms:60000}",
            initialDelayString="${cyys.file-cleanup.interval-ms:60000}")
    public void run() { cleanup.runBatch(); }
}
