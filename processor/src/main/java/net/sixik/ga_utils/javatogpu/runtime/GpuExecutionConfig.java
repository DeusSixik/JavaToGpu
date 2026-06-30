package net.sixik.ga_utils.javatogpu.runtime;

/**
 * Explicit execution configuration for runtime kernel launches.
 *
 * <p>The current runtime contract uses a 1D global work size. This type exists so the public API can grow into a
 * richer execution model later without forcing callers to stay on raw scalar overloads.
 */
public record GpuExecutionConfig(long globalWorkSize) {

    public GpuExecutionConfig {
        if (globalWorkSize <= 0L) {
            throw new IllegalArgumentException("globalWorkSize must be positive: " + globalWorkSize);
        }
    }

    public static GpuExecutionConfig oneDimensional(long globalWorkSize) {
        return new GpuExecutionConfig(globalWorkSize);
    }
}
