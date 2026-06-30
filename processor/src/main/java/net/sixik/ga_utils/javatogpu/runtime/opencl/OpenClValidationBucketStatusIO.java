package net.sixik.ga_utils.javatogpu.runtime.opencl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

final class OpenClValidationBucketStatusIO {

    private OpenClValidationBucketStatusIO() {
    }

    static Map<String, OpenClValidationBucketStatus> readAll(Path path) throws IOException {
        Map<String, OpenClValidationBucketStatus> result = new LinkedHashMap<>();
        if (!Files.exists(path)) {
            return result;
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        }
        java.util.TreeSet<String> prefixes = new java.util.TreeSet<>();
        for (String key : properties.stringPropertyNames()) {
            int separator = key.indexOf('.');
            if (separator > 0) {
                prefixes.add(key.substring(0, separator));
            }
        }
        for (String prefix : prefixes) {
            String taskName = properties.getProperty(prefix + ".taskName", prefix);
            String status = properties.getProperty(prefix + ".status", "unknown");
            Instant recordedAtUtc = Instant.parse(properties.getProperty(prefix + ".recordedAtUtc"));
            result.put(taskName, new OpenClValidationBucketStatus(taskName, status, recordedAtUtc));
        }
        return result;
    }

    static void writeAll(Path path, Map<String, OpenClValidationBucketStatus> statuses) throws IOException {
        Properties properties = new Properties();
        for (Map.Entry<String, OpenClValidationBucketStatus> entry : statuses.entrySet()) {
            String prefix = sanitizeKey(entry.getKey());
            OpenClValidationBucketStatus status = entry.getValue();
            properties.setProperty(prefix + ".taskName", status.taskName());
            properties.setProperty(prefix + ".status", status.status());
            properties.setProperty(prefix + ".recordedAtUtc", status.recordedAtUtc().toString());
        }
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            properties.store(outputStream, "JavaToGpu OpenCL validation bucket status registry");
        }
    }

    private static String sanitizeKey(String taskName) {
        return taskName.replaceAll("[^a-zA-Z0-9]+", "_");
    }
}
