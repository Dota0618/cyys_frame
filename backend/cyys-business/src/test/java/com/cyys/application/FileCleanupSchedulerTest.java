package com.cyys.application;

import com.cyys.admin.file.service.FileCleanupScheduler;
import com.cyys.admin.file.service.FileCleanupService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FileCleanupSchedulerTest {
    @Test void enabledSchedulerActuallyRunsAndCanBeDisabledForMaintenance() {
        var cleanup=mock(FileCleanupService.class);
        var application=new ApplicationContextRunner()
                .withBean(FileCleanupService.class,()->cleanup)
                .withUserConfiguration(FileCleanupScheduler.class)
                .withPropertyValues("cyys.file-cleanup.interval-ms=1000");
        application.run(context -> {
            assertThat(context).hasSingleBean(FileCleanupScheduler.class);
            verify(cleanup,timeout(4000).atLeastOnce()).runBatch();
        });
        clearInvocations(cleanup);
        application.withPropertyValues("cyys.file-cleanup.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(FileCleanupScheduler.class);
            verifyNoInteractions(cleanup);
        });
    }
}
