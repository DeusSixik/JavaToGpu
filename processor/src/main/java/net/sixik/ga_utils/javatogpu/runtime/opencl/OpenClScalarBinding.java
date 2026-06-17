package net.sixik.ga_utils.javatogpu.runtime.opencl;

import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess;

public record OpenClScalarBinding(
        OpenClArgumentKind kind,
        GpuKernelParameterAccess access,
        Object value
) {
}
