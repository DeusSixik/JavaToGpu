package net.sixik.ga_utils.javatogpu.runtime.opencl;

import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess;

public record OpenClScalarArgument(
        OpenClArgumentKind kind,
        GpuKernelParameterAccess access,
        Object value
) implements OpenClKernelArgument {
}
