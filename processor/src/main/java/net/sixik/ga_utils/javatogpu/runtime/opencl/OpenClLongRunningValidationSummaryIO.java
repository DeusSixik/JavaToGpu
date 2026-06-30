package net.sixik.ga_utils.javatogpu.runtime.opencl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;

final class OpenClLongRunningValidationSummaryIO {

    private OpenClLongRunningValidationSummaryIO() {
    }

    static void write(Path path, OpenClLongRunningValidationSummary summary) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("completedAtUtc", summary.completedAtUtc().toString());
        properties.setProperty("status", summary.status());
        properties.setProperty("iterations", Long.toString(summary.iterations()));
        properties.setProperty("invocationCount", Long.toString(summary.statistics().invocationCount()));
        properties.setProperty("compileCount", Long.toString(summary.statistics().compileCount()));
        properties.setProperty("compileCacheHitCount", Long.toString(summary.statistics().compileCacheHitCount()));
        properties.setProperty("sessionCreationCount", Long.toString(summary.statistics().sessionCreationCount()));
        properties.setProperty("deviceBufferCreationCount", Long.toString(summary.statistics().deviceBufferCreationCount()));
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            properties.store(outputStream, "JavaToGpu OpenCL long-running validation summary");
        }
    }

    static Optional<OpenClLongRunningValidationSummary> readIfExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
        }
        return Optional.of(new OpenClLongRunningValidationSummary(
                Instant.parse(properties.getProperty("completedAtUtc")),
                properties.getProperty("status", "unknown"),
                Long.parseLong(properties.getProperty("iterations", "0")),
                new OpenClRuntimeStatistics(
                        Long.parseLong(properties.getProperty("invocationCount", "0")),
                        Long.parseLong(properties.getProperty("compileCount", "0")),
                        Long.parseLong(properties.getProperty("compileCacheHitCount", "0")),
                        Long.parseLong(properties.getProperty("sessionCreationCount", "0")),
                        Long.parseLong(properties.getProperty("deviceBufferCreationCount", "0"))
                )
        ));
    }
}
