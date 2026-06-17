package net.sixik.ga_utils.javatogpu.runtime.opencl;

import dev.denismasterherobrine.packager.opencl.core.OpenClBuffer;
import dev.denismasterherobrine.packager.opencl.core.OpenClCommandQueue;
import dev.denismasterherobrine.packager.opencl.core.OpenClContext;
import dev.denismasterherobrine.packager.opencl.core.OpenClDevice;
import dev.denismasterherobrine.packager.opencl.core.OpenClDevices;
import dev.denismasterherobrine.packager.opencl.core.OpenClKernel;
import dev.denismasterherobrine.packager.opencl.core.OpenClProgram;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelDescriptor;

public final class OpenClRuntimeSession implements AutoCloseable {

    private final OpenClDevice device;
    private final OpenClContext context;
    private final OpenClCommandQueue queue;

    private OpenClRuntimeSession(OpenClDevice device, OpenClContext context, OpenClCommandQueue queue) {
        this.device = device;
        this.context = context;
        this.queue = queue;
    }

    public static OpenClRuntimeSession createDefault() {
        OpenClDevice device = OpenClDevices.selectBest();
        if (device == null) {
            throw new IllegalStateException("No OpenCL device found");
        }

        OpenClContext context = OpenClContext.create(device);
        OpenClCommandQueue queue = context.createQueue(true);
        return new OpenClRuntimeSession(device, context, queue);
    }

    public OpenClCompiledKernel compileKernel(GpuKernelDescriptor descriptor) {
        OpenClProgram program = context.buildProgram(descriptor.kernelSource());
        OpenClKernel kernel = program.createKernel(descriptor.kernelName());
        return new OpenClCompiledKernel(descriptor, descriptor.kernelResource(), program, kernel);
    }

    public OpenClBuffer createReadWriteBuffer(long sizeBytes) {
        return context.createReadWriteBuffer(sizeBytes);
    }

    public OpenClCommandQueue queue() {
        return queue;
    }

    @Override
    public void close() {
        Throwable failure = null;

        try {
            queue.close();
        } catch (Throwable throwable) {
            failure = throwable;
        }

        try {
            context.close();
        } catch (Throwable throwable) {
            if (failure != null) {
                failure.addSuppressed(throwable);
            } else {
                failure = throwable;
            }
        }

        if (failure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (failure != null) {
            throw new RuntimeException("Failed to close OpenCL runtime session for " + device.label(), failure);
        }
    }
}
