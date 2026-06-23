package net.sixik.ga_utils.javatogpu.runtime;

/**
 * Backend/runtime capabilities that higher-level code may require before selecting a GPU backend.
 */
public enum GpuRuntimeFeature {
    DOUBLE_PRECISION,
    IMAGES,
    IMAGE3D_WRITES,
    SHARED_CACHE
}
