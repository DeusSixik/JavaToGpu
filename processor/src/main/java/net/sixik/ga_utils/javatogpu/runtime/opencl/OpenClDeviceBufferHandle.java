package net.sixik.ga_utils.javatogpu.runtime.opencl;

import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess;

public record OpenClDeviceBufferHandle(
        String handleId,
        OpenClArgumentKind kind,
        GpuKernelParameterAccess access,
        Object sourceArray,
        int length
) {
}
