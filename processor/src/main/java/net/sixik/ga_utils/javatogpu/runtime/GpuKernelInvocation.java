package net.sixik.ga_utils.javatogpu.runtime;

public record GpuKernelInvocation(
        GpuKernelDescriptor descriptor,
        Object[] arguments,
        GpuExecutionConfig executionConfig
) {

    public GpuKernelInvocation(GpuKernelDescriptor descriptor, Object[] arguments) {
        this(descriptor, arguments, null);
    }

    public GpuKernelInvocation(GpuKernelDescriptor descriptor, Object[] arguments, long globalWorkSize) {
        this(descriptor, arguments, GpuExecutionConfig.oneDimensional(globalWorkSize));
    }

    public Long globalWorkSize() {
        return executionConfig == null ? null : executionConfig.globalWorkSize();
    }
}
