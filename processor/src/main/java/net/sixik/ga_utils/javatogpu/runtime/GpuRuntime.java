package net.sixik.ga_utils.javatogpu.runtime;

public final class GpuRuntime {

    private static final GpuRuntimeBackend DEFAULT_BACKEND = invocation -> {
        throw new UnsupportedOperationException(
                "GPU runtime backend is not configured for kernel " + invocation.descriptor().kernelName()
        );
    };

    private static volatile GpuRuntimeBackend backend = DEFAULT_BACKEND;

    private GpuRuntime() {
    }

    public static GpuRuntimeBackend defaultBackend() {
        return DEFAULT_BACKEND;
    }

    public static GpuRuntimeBackend backend() {
        return backend;
    }

    public static void setBackend(GpuRuntimeBackend newBackend) {
        backend = newBackend;
    }

    public static void invoke(GpuKernelDescriptor descriptor, Object... arguments) {
        backend.invoke(new GpuKernelInvocation(descriptor, arguments));
    }
}
