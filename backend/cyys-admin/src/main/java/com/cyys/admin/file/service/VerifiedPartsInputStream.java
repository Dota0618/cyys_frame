package com.cyys.admin.file.service;

import com.cyys.admin.file.mapper.FilePartMapper;
import com.cyys.common.storage.ObjectStorage;
import java.io.*;
import java.security.*;
import java.util.*;

/** 有界内存串联分片，消费到底时校验每片长度和摘要。 */
final class VerifiedPartsInputStream extends InputStream {
    private final ObjectStorage storage;
    private final List<FilePartMapper.Part> parts;
    private final long expectedBytes;
    private final int partBytes;
    private int index;
    private InputStream current;
    private MessageDigest digest;
    private long count;

    VerifiedPartsInputStream(ObjectStorage storage, List<FilePartMapper.Part> parts, long expectedBytes, int partBytes) {
        this.storage = storage; this.parts = parts; this.expectedBytes = expectedBytes; this.partBytes = partBytes;
    }
    @Override public int read() throws IOException {
        byte[] one = new byte[1];
        return read(one, 0, 1) == -1 ? -1 : one[0] & 0xff;
    }
    @Override public int read(byte[] buffer, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, buffer.length);
        if (length == 0) return 0;
        while (index < parts.size()) {
            if (current == null) {
                try { current = storage.read(parts.get(index).objectKey()); }
                catch (java.nio.file.NoSuchFileException missing) { throw new CorruptFilePartException(index); }
                count = 0;
                try { digest = MessageDigest.getInstance("SHA-256"); }
                catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
            }
            int read = current.read(buffer, offset, length);
            if (read != -1) { count += read; digest.update(buffer, offset, read); return read; }
            current.close(); current = null;
            if (count != Math.min(partBytes, expectedBytes - (long) index * partBytes)
                    || !HexFormat.of().formatHex(digest.digest()).equals(parts.get(index).expectedSha256())) {
                throw new CorruptFilePartException(index);
            }
            index++;
        }
        return -1;
    }
    @Override public void close() throws IOException { if (current != null) current.close(); }
}
