package net.sixik.ga_utils.javatogpu.runtime;

public record GpuKernelInvocation(
        GpuKernelDescriptor descriptor,
        Object[] arguments
) {
}
