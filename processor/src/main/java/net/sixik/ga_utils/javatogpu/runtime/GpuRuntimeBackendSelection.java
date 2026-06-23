package net.sixik.ga_utils.javatogpu.runtime;

/**
 * Selected backend together with the capability report that made it eligible.
 */
public record GpuRuntimeBackendSelection(
        GpuRuntimeBackend backend,
        GpuRuntimeBackendReport report
) {
}
