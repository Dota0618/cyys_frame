package com.cyys.common.storage;

import com.cyys.common.web.ApiException;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.*;

/** 扁平对象键避免目录穿越。根目录由部署者预先创建并限制写权限。 */
public final class LocalObjectStorage implements ObjectStorage {
    private final Path root;

    public LocalObjectStorage(Path root) throws IOException {
        if (!root.isAbsolute()) throw new IOException("Storage root must be absolute");
        this.root = root.normalize();
        checkRoot();
    }

    private void checkRoot() throws IOException {
        for (Path part = root; part != null; part = part.getParent()) {
            if (Files.isSymbolicLink(part) || !Files.isDirectory(part, NOFOLLOW_LINKS)
                    || !part.toRealPath().equals(part.toRealPath(NOFOLLOW_LINKS))) {
                throw new IOException("Storage root contains a link or is unavailable");
            }
        }
    }

    private Path object(String key) throws IOException {
        if (key == null || !key.matches("[a-f0-9]{32}")) throw new IOException("Invalid object key");
        checkRoot();
        Path path = root.resolve(key);
        if (Files.exists(path, NOFOLLOW_LINKS) && !Files.isRegularFile(path, NOFOLLOW_LINKS)) {
            throw new IOException("Object is not a regular file");
        }
        return path;
    }

    @Override
    public Written write(String key, InputStream source, long maxBytes) throws IOException {
        if (maxBytes <= 0) throw new IOException("Invalid upload limit");
        Path path = object(key);
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        // CREATE_NEW 失败时绝不清理已有对象。
        OutputStream stream = Files.newOutputStream(path, CREATE_NEW, WRITE, NOFOLLOW_LINKS);
        try (stream) {
            long size = 0;
            byte[] buffer = new byte[8192];
            int count;
            while ((count = source.read(buffer)) != -1) {
                if (count > maxBytes - size) throw new ApiException(413, "文件超过允许大小");
                stream.write(buffer, 0, count);
                digest.update(buffer, 0, count);
                size += count;
            }
            return new Written(size, HexFormat.of().formatHex(digest.digest()));
        } catch (IOException | RuntimeException error) {
            try { Files.delete(path); } catch (IOException cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
    }

    @Override public InputStream read(String key) throws IOException {
        return Files.newInputStream(object(key), READ, NOFOLLOW_LINKS);
    }

    @Override public void delete(String key) throws IOException {
        Files.deleteIfExists(object(key));
    }
}
