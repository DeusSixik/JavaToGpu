package net.sixik.ga_utils.javatogpu.runtime;

/**
 * Predicate-like requirement used for backend capability matching.
 *
 * <p>Implementations return {@code null} when a report satisfies the requirement and a human-readable explanation when
 * it does not.
 */
@FunctionalInterface
public interface GpuRuntimeRequirement {

    /**
     * Returns {@code null} when the requirement is satisfied or a human-readable failure reason otherwise.
     */
    String failureReason(GpuRuntimeBackendReport report);
}
