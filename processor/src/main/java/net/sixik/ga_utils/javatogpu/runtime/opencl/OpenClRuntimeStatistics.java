package net.sixik.ga_utils.javatogpu.runtime.opencl;

/**
 * Immutable snapshot of lightweight OpenCL backend runtime counters.
 *
 * <p>The counters are intended for production diagnostics, cache warm-up validation, and stress-test assertions.
 * They are deliberately simple so callers can cheaply sample them around repeated kernel runs.
 */
public record OpenClRuntimeStatistics(
        long invocationCount,
        long compileCount,
        long compileCacheHitCount,
        long sessionCreationCount,
        long deviceBufferCreationCount
) {
}
