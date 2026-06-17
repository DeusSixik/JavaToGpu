package net.sixik.ga_utils.javatogpu.runtime.opencl;

import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess;

public record OpenClArrayArgument(
        OpenClArgumentKind kind,
        GpuKernelParameterAccess access,
        Object sourceArray,
        int length
) implements OpenClKernelArgument {
}
