package net.sixik.ga_utils.javatogpu.runtime;

@FunctionalInterface
public interface GpuRuntimeBackend {

    void invoke(GpuKernelInvocation invocation);
}
