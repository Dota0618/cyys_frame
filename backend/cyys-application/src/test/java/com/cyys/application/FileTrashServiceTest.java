package com.cyys.application;

import com.cyys.admin.audit.service.DataAuditService;
import com.cyys.admin.file.dto.FileVersionReference;
import com.cyys.admin.file.mapper.FileEntryMapper;
import com.cyys.admin.file.mapper.FileOwnerMapper;
import com.cyys.admin.file.model.FileEntry;
import com.cyys.admin.file.service.*;
import com.cyys.admin.configuration.service.SystemRuntimeSettings;
import com.cyys.common.satoken.RequestIdentity;
import com.cyys.common.satoken.ScopeContext;
import com.cyys.common.storage.ConfiguredStorage;
import com.cyys.common.web.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileTrashServiceTest {
    private final FileEntryMapper mapper = mock(FileEntryMapper.class);
    private final FileAttachmentService attachments = mock(FileAttachmentService.class);
    private final TemporaryFileCleaner cleaner = mock(TemporaryFileCleaner.class);
    private final DataAuditService audit = mock(DataAuditService.class);
    private final FileService service = new FileService(mapper, mock(FileOwnerMapper.class),
            mock(ConfiguredStorage.class), audit, cleaner, mock(FileDirectoryService.class), attachments,
            mock(ResumableUploadService.class), mock(FileRecycleService.class), mock(PlatformTransactionManager.class),
            mock(SystemRuntimeSettings.class));

    @BeforeEach void identity() {
        ScopeContext.set(new RequestIdentity("actor", "scope-a", List.of(), List.of()));
    }

    @AfterEach void clear() { ScopeContext.clear(); }

    @Test void batchValidatesAllFilesOnceBeforeCleaningOrDeleting() {
        var chunked = file("a", "READY");
        chunked.setPartBytes(1024);
        var failed = file("b", "FAILED");
        var ready = file("c", "READY");
        service.trashBatch(List.of(reference("c"), reference("b"), reference("a")));

        var order = inOrder(mapper, attachments, cleaner, audit);
        for (var file : List.of(chunked, failed, ready)) {
            order.verify(mapper).lock(file.getId());
            order.verify(attachments).requireUnbound(file);
        }
        for (var file : List.of(chunked, failed, ready)) {
            if (file != ready) {
                order.verify(cleaner).clean(file);
                order.verify(mapper).temporaryCleaned(file.getId(), 2);
            }
            order.verify(mapper).trash(file.getId(), 2, "actor");
            order.verify(audit).success("file", file.getId(), "DELETE");
        }
        verifyNoMoreInteractions(mapper, attachments, cleaner, audit);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "version", "uploading", "bound"})
    void invalidLaterFilePreventsAllCleanupAndWrites(String failure) {
        var first = file("a", "FAILED");
        var invalid = file("b", "READY");
        switch (failure) {
            case "missing" -> when(mapper.lock("b")).thenReturn(null);
            case "version" -> invalid.setVersion(3);
            case "uploading" -> invalid.setState("UPLOADING");
            case "bound" -> doThrow(new ApiException(409, "文件仍被引用")).when(attachments).requireUnbound(invalid);
            default -> throw new AssertionError(failure);
        }
        assertThatThrownBy(() -> service.trashBatch(List.of(reference("b"), reference("a"))))
                .isInstanceOfSatisfying(ApiException.class, error -> assertThat(error.getStatus()).isEqualTo(409));
        verify(mapper).lock("a");
        verify(mapper).lock("b");
        verify(attachments).requireUnbound(first);
        verify(mapper, never()).temporaryCleaned(anyString(), anyInt());
        verify(mapper, never()).trash(anyString(), anyInt(), anyString());
        verifyNoInteractions(cleaner, audit);
    }

    @Test void duplicateFilesFailBeforeLocking() {
        assertThatThrownBy(() -> service.trashBatch(List.of(reference("a"), reference("a"))))
                .hasMessageContaining("重复文件");
        verifyNoInteractions(mapper, attachments, cleaner, audit);
    }

    @Test void cleanupFailureNeverMarksFileAsDeletedOrAuditsSuccess() {
        var failed = file("a", "FAILED");
        doThrow(new ApiException(503, "清理失败")).when(cleaner).clean(failed);
        assertThatThrownBy(() -> service.trash("a", 2)).hasMessageContaining("清理失败");
        verify(mapper, never()).temporaryCleaned(anyString(), anyInt());
        verify(mapper, never()).trash(anyString(), anyInt(), anyString());
        verifyNoInteractions(audit);
    }

    @Test void singleReadyFileUsesTheSameValidationWithoutTemporaryCleanup() {
        var ready = file("a", "READY");
        service.trash("a", 2);
        verify(mapper).lock("a");
        verify(attachments).requireUnbound(ready);
        verify(mapper).trash("a", 2, "actor");
        verify(audit).success("file", "a", "DELETE");
        verifyNoInteractions(cleaner);
        verifyNoMoreInteractions(mapper, attachments, audit);
    }

    private FileEntry file(String id, String state) {
        var file = new FileEntry();
        file.setId(id);
        file.setState(state);
        file.setVersion(2);
        when(mapper.lock(id)).thenReturn(file);
        when(mapper.temporaryCleaned(id, 2)).thenReturn(1);
        when(mapper.trash(id, 2, "actor")).thenReturn(1);
        return file;
    }

    private FileVersionReference reference(String id) { return new FileVersionReference(id, 2); }
}
