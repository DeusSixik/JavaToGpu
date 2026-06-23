package net.sixik.ga_utils.javatogpu.runtime;

/**
 * Creates runtime backends on demand for capability probing and fallback selection.
 */
@FunctionalInterface
public interface GpuRuntimeBackendFactory {

    /**
     * Creates one backend instance.
     */
    GpuRuntimeBackend create();
}
