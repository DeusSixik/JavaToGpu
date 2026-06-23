package net.sixik.ga_utils.javatogpu.runtime;

import net.sixik.ga_utils.javatogpu.api.GpuBackendTarget;
import net.sixik.ga_utils.javatogpu.runtime.opencl.OpenClGpuRuntimeBackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable backend selection policy that combines capability requirements with an ordered fallback chain.
 *
 * <p>This is a convenience layer on top of {@link GpuRuntimeRequirement} and {@link GpuRuntimeBackendFactory}. It is
 * meant for application code that wants to express preference order once and then reuse it repeatedly.
 *
 * <pre>{@code
 * GpuRuntimeBackendPolicy policy = GpuRuntimeBackendPolicy.builder()
 *         .minimumApiVersion(GpuBackendTarget.OPENCL, 3, 0)
 *         .requireFeature(GpuBackendTarget.OPENCL, GpuRuntimeFeature.IMAGES)
 *         .preferOpenClSharedCache()
 *         .build();
 *
 * try (GpuRuntimeScope ignored = GpuRuntime.use(policy)) {
 *     DemoKernel.invoke(input, output);
 * }
 * }</pre>
 */
public final class GpuRuntimeBackendPolicy {

    private final List<GpuRuntimeRequirement> requirements;
    private final List<GpuRuntimeBackendFactory> candidateFactories;

    private GpuRuntimeBackendPolicy(
            List<GpuRuntimeRequirement> requirements,
            List<GpuRuntimeBackendFactory> candidateFactories
    ) {
        this.requirements = List.copyOf(requirements);
        this.candidateFactories = List.copyOf(candidateFactories);
    }

    /**
     * Creates a new mutable builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the ordered capability requirements applied to every candidate report.
     */
    public List<GpuRuntimeRequirement> requirements() {
        return requirements;
    }

    /**
     * Returns the ordered fallback chain used for backend creation and selection.
     */
    public List<GpuRuntimeBackendFactory> candidateFactories() {
        return candidateFactories;
    }

    /**
     * Creates backend candidates, returns the first matching backend, and leaves ownership of that backend to the
     * caller.
     *
     * <p>Rejected candidates are closed automatically when possible. If the returned backend implements
     * {@link AutoCloseable}, the caller is responsible for closing it.
     */
    public GpuRuntimeBackendSelection select() {
        List<String> failures = new ArrayList<>();
        for (GpuRuntimeBackendFactory factory : candidateFactories) {
            GpuRuntimeBackend candidate;
            try {
                candidate = factory.create();
            } catch (RuntimeException exception) {
                failures.add("Failed to create backend candidate: " + exception.getMessage());
                continue;
            }

            GpuRuntimeBackendReport report = GpuRuntime.describeBackend(candidate);
            List<String> reasons = GpuRuntimeRequirements.failureReasons(report, requirements);
            if (reasons.isEmpty()) {
                return new GpuRuntimeBackendSelection(candidate, report);
            }

            failures.add(report.backendName() + ": " + String.join("; ", reasons));
            closeCandidateQuietly(candidate);
        }

        throw new UnsupportedOperationException(
                "No GPU runtime backend satisfies the requested requirements: " + String.join(" | ", failures)
        );
    }

    /**
     * Creates backend candidates, installs the first matching backend as an owned runtime scope, and closes rejected
     * candidates automatically when possible.
     */
    public GpuRuntimeScope use() {
        return GpuRuntime.useFirstMatching(
                requirements,
                candidateFactories.toArray(GpuRuntimeBackendFactory[]::new)
        );
    }

    private static void closeCandidateQuietly(GpuRuntimeBackend candidate) {
        if (candidate instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // Best effort cleanup for rejected candidates.
            }
        }
    }

    /**
     * Fluent builder for {@link GpuRuntimeBackendPolicy}.
     */
    public static final class Builder {

        private final List<GpuRuntimeRequirement> requirements = new ArrayList<>();
        private final List<GpuRuntimeBackendFactory> candidateFactories = new ArrayList<>();

        private Builder() {
        }

        /**
         * Adds one capability requirement.
         */
        public Builder require(GpuRuntimeRequirement requirement) {
            requirements.add(Objects.requireNonNull(requirement, "requirement"));
            return this;
        }

        /**
         * Adds a requirement for the given feature on any backend.
         */
        public Builder requireFeature(GpuRuntimeFeature feature) {
            return require(GpuRuntimeRequirements.requireFeature(feature));
        }

        /**
         * Adds a requirement for the given feature on the specified backend family.
         */
        public Builder requireFeature(GpuBackendTarget backendTarget, GpuRuntimeFeature feature) {
            return require(GpuRuntimeRequirements.requireFeature(backendTarget, feature));
        }

        /**
         * Requires at least the given API version for the specified backend family.
         */
        public Builder minimumApiVersion(GpuBackendTarget backendTarget, int major, int minor) {
            return require(GpuRuntimeRequirements.minimumApiVersion(backendTarget, major, minor));
        }

        /**
         * Requires at least the given amount of local memory on any backend.
         */
        public Builder minimumLocalMemoryBytes(long bytes) {
            return require(GpuRuntimeRequirements.minimumLocalMemoryBytes(bytes));
        }

        /**
         * Requires at least the given amount of local memory for the specified backend family.
         */
        public Builder minimumLocalMemoryBytes(GpuBackendTarget backendTarget, long bytes) {
            return require(GpuRuntimeRequirements.minimumLocalMemoryBytes(backendTarget, bytes));
        }

        /**
         * Requires at least the given maximum work-group size on any backend.
         */
        public Builder minimumMaxWorkGroupSize(long size) {
            return require(GpuRuntimeRequirements.minimumMaxWorkGroupSize(size));
        }

        /**
         * Requires at least the given maximum work-group size for the specified backend family.
         */
        public Builder minimumMaxWorkGroupSize(GpuBackendTarget backendTarget, long size) {
            return require(GpuRuntimeRequirements.minimumMaxWorkGroupSize(backendTarget, size));
        }

        /**
         * Appends one backend factory to the fallback chain.
         */
        public Builder prefer(GpuRuntimeBackendFactory factory) {
            candidateFactories.add(Objects.requireNonNull(factory, "factory"));
            return this;
        }

        /**
         * Appends one already-constructed backend to the fallback chain.
         */
        public Builder preferBackend(GpuRuntimeBackend backend) {
            Objects.requireNonNull(backend, "backend");
            candidateFactories.add(() -> backend);
            return this;
        }

        /**
         * Appends an instance-local OpenCL backend to the fallback chain.
         */
        public Builder preferOpenCl() {
            candidateFactories.add(OpenClGpuRuntimeBackend::new);
            return this;
        }

        /**
         * Appends a shared-cache OpenCL backend to the fallback chain.
         */
        public Builder preferOpenClSharedCache() {
            candidateFactories.add(OpenClGpuRuntimeBackend::sharedCache);
            return this;
        }

        /**
         * Builds an immutable backend selection policy.
         */
        public GpuRuntimeBackendPolicy build() {
            if (candidateFactories.isEmpty()) {
                throw new IllegalStateException("GPU runtime backend policy requires at least one candidate backend");
            }
            return new GpuRuntimeBackendPolicy(requirements, candidateFactories);
        }
    }
}
