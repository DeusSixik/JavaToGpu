package net.sixik.ga_utils.javatogpu.runtime.opencl;

import java.time.Instant;
import java.util.Objects;

/**
 * Summary artifact written by the long-running real-device OpenCL stability bucket.
 */
public record OpenClLongRunningValidationSummary(
        Instant completedAtUtc,
        String status,
        long iterations,
        OpenClRuntimeStatistics statistics
) {

    public OpenClLongRunningValidationSummary {
        completedAtUtc = Objects.requireNonNull(completedAtUtc, "completedAtUtc");
        status = Objects.requireNonNull(status, "status");
        statistics = Objects.requireNonNull(statistics, "statistics");
    }
}
