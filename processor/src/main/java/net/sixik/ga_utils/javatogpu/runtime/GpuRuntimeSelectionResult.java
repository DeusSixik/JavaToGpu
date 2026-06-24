package net.sixik.ga_utils.javatogpu.runtime;

import java.util.List;

/**
 * Result of a capability-based backend selection attempt.
 *
 * <p>This is the non-throwing companion to {@link GpuRuntimeBackendSelection}. It is useful for production flows that
 * want to perform a capability precheck and then decide whether to install a backend, skip GPU execution, or fall back
 * to another path without treating the miss as an exceptional control-flow event.
 */
public record GpuRuntimeSelectionResult(
        GpuRuntimeBackendSelection selection,
        List<String> failureReasons
) {

    public GpuRuntimeSelectionResult {
        failureReasons = failureReasons == null ? List.of() : List.copyOf(failureReasons);
    }

    /**
     * Returns {@code true} when a backend matched all requested requirements.
     */
    public boolean matched() {
        return selection != null;
    }

    /**
     * Returns the selected backend selection or throws a descriptive exception when none matched.
     */
    public GpuRuntimeBackendSelection requireSelection() {
        if (selection != null) {
            return selection;
        }
        throw new UnsupportedOperationException(
                "No GPU runtime backend satisfies the requested requirements: " + failureSummary()
        );
    }

    /**
     * Returns all recorded rejection reasons as one human-readable summary.
     */
    public String failureSummary() {
        return String.join(" | ", failureReasons);
    }
}
