package net.sixik.ga_utils.javatogpu.runtime;

import net.sixik.ga_utils.javatogpu.runtime.opencl.OpenClGpuRuntimeBackend;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Global entry point for executing generated GPU kernels at runtime.
 *
 * <p>Generated launchers call into this class with a {@link GpuKernelDescriptor} and the original Java arguments.
 * The actual execution strategy is delegated to the currently configured {@link GpuRuntimeBackend}.
 *
 * <p>Typical usage is to install a backend once for an application scope or test scope:
 *
 * <pre>{@code
 * GpuRuntime.setBackend(net.sixik.ga_utils.javatogpu.runtime.opencl.OpenClGpuRuntimeBackend.sharedCache());
 * try {
 *     DemoKernel.invoke(input, output);
 * } finally {
 *     net.sixik.ga_utils.javatogpu.runtime.opencl.OpenClGpuRuntimeBackend.shutdownSharedCache();
 *     GpuRuntime.setBackend(GpuRuntime.defaultBackend());
 * }
 * }</pre>
 *
 * <p>If no backend is configured, the default backend fails fast with a descriptive exception.
 */
public final class GpuRuntime {

    private static final GpuRuntimeBackend DEFAULT_BACKEND = invocation -> {
        throw new UnsupportedOperationException(
                "GPU runtime backend is not configured for kernel " + invocation.descriptor().kernelName()
        );
    };

    private static volatile GpuRuntimeBackend backend = DEFAULT_BACKEND;

    private GpuRuntime() {
    }

    /**
     * Returns the default fail-fast backend.
     *
     * <p>This backend does not execute kernels. It exists so direct GPU launcher calls fail with a predictable error
     * when the application forgot to install a real runtime backend.
     */
    public static GpuRuntimeBackend defaultBackend() {
        return DEFAULT_BACKEND;
    }

    /**
     * Returns the backend currently used for GPU kernel execution.
     */
    public static GpuRuntimeBackend backend() {
        return backend;
    }

    /**
     * Replaces the active runtime backend.
     *
     * @param newBackend backend that should handle subsequent GPU kernel invocations
     */
    public static void setBackend(GpuRuntimeBackend newBackend) {
        backend = newBackend;
    }

    /**
     * Installs a backend for the current scope and restores the previous backend when the returned scope is closed.
     *
     * <p>The installed backend is not automatically closed by this method.
     *
     * @param newBackend backend that should be active while the returned scope is open
     * @return scope that restores the previous backend on close
     */
    public static GpuRuntimeScope useBackend(GpuRuntimeBackend newBackend) {
        return installScopedBackend(newBackend, false);
    }

    /**
     * Installs a backend for the current scope and closes it when the returned scope is closed, if it implements
     * {@link AutoCloseable}.
     *
     * @param newBackend backend that should be active while the returned scope is open
     * @return scope that restores the previous backend and closes the installed backend on close
     */
    public static GpuRuntimeScope useOwnedBackend(GpuRuntimeBackend newBackend) {
        return installScopedBackend(newBackend, true);
    }

    /**
     * Installs a fresh instance-local OpenCL backend for the current scope.
     *
     * <p>When the returned scope is closed, the previous backend is restored and the OpenCL backend instance is closed.
     */
    public static GpuRuntimeScope useOpenCl() {
        return useOwnedBackend(new OpenClGpuRuntimeBackend());
    }

    /**
     * Installs a shared-cache OpenCL backend for the current scope.
     *
     * <p>When the scope is closed, the previous backend is restored and the backend instance itself is closed, but the
     * shared OpenCL compile/session cache remains warm. Use {@link #shutdownOpenClSharedCache()} to explicitly release
     * that global cache.
     */
    public static GpuRuntimeScope useOpenClSharedCache() {
        return useOwnedBackend(OpenClGpuRuntimeBackend.sharedCache());
    }

    /**
     * Releases the global shared OpenCL cache created by {@link #useOpenClSharedCache()}.
     */
    public static void shutdownOpenClSharedCache() {
        OpenClGpuRuntimeBackend.shutdownSharedCache();
    }

    /**
     * Returns a capability report for the given backend instance.
     */
    public static GpuRuntimeBackendReport describeBackend(GpuRuntimeBackend backend) {
        return backend.describeCapabilities();
    }

    /**
     * Selects the first backend whose capability report satisfies the given requirements.
     *
     * <p>Backends are checked in the order provided. This lets callers express preference order such as
     * {@code CUDA -> OpenCL -> custom fallback}.
     *
     * @throws UnsupportedOperationException when no backend satisfies the requirements
     */
    public static GpuRuntimeBackendSelection selectFirstMatching(
            List<GpuRuntimeRequirement> requirements,
            GpuRuntimeBackend... candidates
    ) {
        List<String> failures = new ArrayList<>();
        for (GpuRuntimeBackend candidate : candidates) {
            GpuRuntimeBackendReport report = describeBackend(candidate);
            List<String> reasons = GpuRuntimeRequirements.failureReasons(report, requirements);
            if (reasons.isEmpty()) {
                return new GpuRuntimeBackendSelection(candidate, report);
            }
            failures.add(report.backendName() + ": " + String.join("; ", reasons));
        }
        throw new UnsupportedOperationException(
                "No GPU runtime backend satisfies the requested requirements: " + String.join(" | ", failures)
        );
    }

    /**
     * Selects the first available backend without additional requirements.
     */
    public static GpuRuntimeBackendSelection selectFirstAvailable(GpuRuntimeBackend... candidates) {
        return selectFirstMatching(List.of(), candidates);
    }

    /**
     * Selects a backend using an immutable fallback policy.
     */
    public static GpuRuntimeBackendSelection select(GpuRuntimeBackendPolicy policy) {
        return Objects.requireNonNull(policy, "policy").select();
    }

    /**
     * Creates backend candidates from factories, selects the first matching backend, installs it as an owned scope,
     * and closes rejected backend instances automatically when possible.
     *
     * @throws UnsupportedOperationException when no backend satisfies the requirements
     */
    public static GpuRuntimeScope useFirstMatching(
            List<GpuRuntimeRequirement> requirements,
            GpuRuntimeBackendFactory... candidateFactories
    ) {
        List<String> failures = new ArrayList<>();
        for (GpuRuntimeBackendFactory factory : candidateFactories) {
            GpuRuntimeBackend candidate;
            try {
                candidate = factory.create();
            } catch (RuntimeException exception) {
                failures.add("Failed to create backend candidate: " + exception.getMessage());
                continue;
            }

            GpuRuntimeBackendReport report = describeBackend(candidate);
            List<String> reasons = GpuRuntimeRequirements.failureReasons(report, requirements);
            if (reasons.isEmpty()) {
                return useOwnedBackend(candidate);
            }

            failures.add(report.backendName() + ": " + String.join("; ", reasons));
            closeCandidateQuietly(candidate);
        }
        throw new UnsupportedOperationException(
                "No GPU runtime backend satisfies the requested requirements: " + String.join(" | ", failures)
        );
    }

    /**
     * Creates backend candidates from factories and installs the first available one as an owned scope.
     */
    public static GpuRuntimeScope useFirstAvailable(GpuRuntimeBackendFactory... candidateFactories) {
        return useFirstMatching(List.of(), candidateFactories);
    }

    /**
     * Installs the first backend selected by the given immutable fallback policy.
     */
    public static GpuRuntimeScope use(GpuRuntimeBackendPolicy policy) {
        return Objects.requireNonNull(policy, "policy").use();
    }

    /**
     * Invokes a generated GPU kernel through the currently configured backend.
     *
     * @param descriptor generated kernel descriptor containing the kernel name, source, and parameter metadata
     * @param arguments original Java launch arguments in generated launcher order
     */
    public static void invoke(GpuKernelDescriptor descriptor, Object... arguments) {
        backend.invoke(new GpuKernelInvocation(descriptor, arguments));
    }

    private static GpuRuntimeScope installScopedBackend(GpuRuntimeBackend newBackend, boolean closeInstalledBackend) {
        GpuRuntimeBackend previousBackend = backend();
        setBackend(newBackend);
        return new GpuRuntimeScope(previousBackend, newBackend, closeInstalledBackend);
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
}
