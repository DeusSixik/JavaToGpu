package net.sixik.ga_utils.javatogpu.runtime.opencl;

import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess;

public record OpenClPreparedBufferBinding(
        OpenClBufferBinding binding,
        OpenClDeviceBufferHandle handle
) {
    public GpuKernelParameterAccess access() {
        return binding.access();
    }
}
