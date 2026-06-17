package net.sixik.ga_utils.javatogpu.runtime;

public record GpuKernelParameterDescriptor(
        String name,
        String javaType,
        GpuKernelParameterAccess access
) {
}
