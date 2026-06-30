package net.sixik.ga_utils.javatogpu.runtime.opencl;

import java.time.Instant;
import java.util.Objects;

record OpenClValidationBucketStatus(
        String taskName,
        String status,
        Instant recordedAtUtc
) {

    OpenClValidationBucketStatus {
        taskName = Objects.requireNonNull(taskName, "taskName");
        status = Objects.requireNonNull(status, "status");
        recordedAtUtc = Objects.requireNonNull(recordedAtUtc, "recordedAtUtc");
    }
}
