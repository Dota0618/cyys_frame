package com.cyys.application;

import com.cyys.common.storage.LocalObjectStorage;
import com.cyys.common.web.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class LocalObjectStorageTest {
    @TempDir Path root;
    private static final String KEY="0123456789abcdef0123456789abcdef";

    @Test void historicalCopyLengthBoundariesPreserveEveryByte() throws Exception {
        var storage = new LocalObjectStorage(root);
        for (int length : new int[]{0, 3, 1023, 1024, 1025}) {
            byte[] bytes = new byte[length];
            new Random(length).nextBytes(bytes);
            assertThat(storage.write(KEY, new ByteArrayInputStream(bytes), Math.max(1, length)).bytes()).isEqualTo(length);
            try (var input = storage.read(KEY)) {
                assertThat(input.readAllBytes()).as("copy length %s", length).isEqualTo(bytes);
            }
            storage.delete(KEY);
        }
    }

    @Test void exactByteCopyAndFailureCleanupDoNotOverwriteExistingObjects() throws Exception {
        var storage=new LocalObjectStorage(root);
        byte[] bytes=new byte[8193]; new Random(10).nextBytes(bytes);
        assertThat(storage.write(KEY,new ByteArrayInputStream(bytes),bytes.length).bytes()).isEqualTo(bytes.length);
        try(var input=storage.read(KEY)) { assertThat(input.readAllBytes()).isEqualTo(bytes); }
        assertThatThrownBy(() -> storage.write(KEY,new ByteArrayInputStream(new byte[0]),10)).isInstanceOf(FileAlreadyExistsException.class);
        assertThat(Files.readAllBytes(root.resolve(KEY))).isEqualTo(bytes);
        storage.delete(KEY);
        assertThatThrownBy(() -> storage.write(KEY,new ByteArrayInputStream(bytes),8192)).isInstanceOf(ApiException.class);
        assertThat(Files.exists(root.resolve(KEY))).isFalse();
        InputStream failing=new InputStream() { public int read() throws IOException { throw new IOException("injected"); } };
        assertThatThrownBy(() -> storage.write(KEY,failing,10)).isInstanceOf(IOException.class);
        assertThat(Files.exists(root.resolve(KEY))).isFalse();
    }

    @Test void pathsDirectoriesAndFailedDeletionAreNeverAccepted() throws Exception {
        var storage=new LocalObjectStorage(root);
        for(String invalid:List.of("../secret","C:\\secret","/secret",KEY+"/child","", "..")) {
            assertThatThrownBy(() -> storage.read(invalid)).isInstanceOf(IOException.class);
            assertThatThrownBy(() -> storage.delete(invalid)).isInstanceOf(IOException.class);
        }
        Files.createDirectory(root.resolve(KEY));
        assertThatThrownBy(() -> storage.delete(KEY)).isInstanceOf(IOException.class);
        assertThat(Files.isDirectory(root.resolve(KEY))).isTrue();
    }
}
