package com.cyys.admin.configuration.service;

import com.cyys.common.web.ApiException;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** 文件扩展名白名单；扩展名只按文件名判断，不信任客户端 MIME 类型。 */
public record FileTypePolicy(Set<String> files, Set<String> media, Set<String> images) {
    private static final Pattern EXTENSION = Pattern.compile("[a-z0-9][a-z0-9+_-]{0,31}");

    public FileTypePolicy {
        files = Set.copyOf(files);
        media = Set.copyOf(media);
        images = Set.copyOf(images);
    }

    public static FileTypePolicy from(String files, String media, String images) {
        return new FileTypePolicy(parseLenient(files), parseLenient(media), parseLenient(images));
    }

    public static void validate(String name, String value) {
        try {
            parse(value);
        } catch (IllegalArgumentException error) {
            throw new ApiException(400, name + " 必须是逗号分隔的文件扩展名，例如 pdf,docx；扩展名不能包含路径或通配符");
        }
    }

    public void requireAllowed(String fileName) {
        if (isUnrestricted()) return;
        String extension = extension(fileName);
        if (!files.contains(extension) && !media.contains(extension) && !images.contains(extension)) {
            throw new ApiException(415, "不允许上传该文件类型");
        }
    }

    private boolean isUnrestricted() {
        return files.isEmpty() && media.isEmpty() && images.isEmpty();
    }

    private static Set<String> parseLenient(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException ignored) {
            return Set.of();
        }
    }

    private static Set<String> parse(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Arrays.stream(value.split("[,;\\s]+"))
                .map(String::strip)
                .filter(token -> !token.isEmpty())
                .map(FileTypePolicy::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalize(String token) {
        String normalized = token.toLowerCase(Locale.ROOT);
        while (normalized.startsWith(".")) normalized = normalized.substring(1);
        if (!EXTENSION.matcher(normalized).matches()) throw new IllegalArgumentException("invalid extension");
        return normalized;
    }

    private static String extension(String fileName) {
        if (fileName == null) return "";
        String name = fileName.strip();
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
