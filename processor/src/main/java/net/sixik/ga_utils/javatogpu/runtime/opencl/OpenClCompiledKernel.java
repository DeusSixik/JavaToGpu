package net.sixik.ga_utils.javatogpu.runtime.opencl;

import dev.denismasterherobrine.packager.opencl.core.OpenClKernel;
import dev.denismasterherobrine.packager.opencl.core.OpenClProgram;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelDescriptor;

public record OpenClCompiledKernel(
        GpuKernelDescriptor descriptor,
        String cacheKey,
        OpenClProgram program,
        OpenClKernel kernel
) implements AutoCloseable {

    public OpenClCompiledKernel(GpuKernelDescriptor descriptor, String cacheKey) {
        this(descriptor, cacheKey, null, null);
    }

    @Override
    public void close() {
        Throwable failure = null;

        if (kernel != null) {
            try {
                kernel.close();
            } catch (Throwable throwable) {
                failure = throwable;
            }
        }

        if (program != null) {
            try {
                program.close();
            } catch (Throwable throwable) {
                if (failure != null) {
                    failure.addSuppressed(throwable);
                } else {
                    failure = throwable;
                }
            }
        }

        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure != null) {
            throw new RuntimeException("Failed to close OpenCL compiled kernel", failure);
        }
    }
}
