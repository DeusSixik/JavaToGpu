package net.sixik.ga_utils.javatogpu.runtime.opencl;

import net.sixik.ga_utils.javatogpu.runtime.GpuKernelDescriptor;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelInvocation;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterDescriptor;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class OpenClGpuRuntimeBackendMarshallingTest {

    @Test
    void backendPassesMarshalledArgumentsToExecuteHook() {
        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "kernel",
                "javatogpu/sample/Demo/kernel.cl",
                "__kernel void kernel() {}",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("input", "float[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("count", "int", GpuKernelParameterAccess.VALUE)
                )
        );
        AtomicReference<OpenClPreparedExecution> capturedExecution = new AtomicReference<>();

        OpenClGpuRuntimeBackend backend = new OpenClGpuRuntimeBackend() {
            @Override
            protected OpenClCompiledKernel compileKernel(GpuKernelDescriptor kernelDescriptor) {
                return new OpenClCompiledKernel(kernelDescriptor, "compiled:test");
            }

            @Override
            protected void executeKernel(OpenClPreparedExecution execution) {
                capturedExecution.set(execution);
            }
        };

        backend.invoke(new GpuKernelInvocation(descriptor, new Object[]{new float[]{1.0f, 2.0f}, 4}));

        OpenClPreparedExecution execution = capturedExecution.get();
        assertEquals(1, execution.bufferBindings().size());
        assertEquals(1, execution.scalarBindings().size());
        OpenClPreparedBufferBinding arrayArgument = assertInstanceOf(OpenClPreparedBufferBinding.class, execution.bufferBindings().get(0));
        assertEquals(OpenClArgumentKind.FLOAT_ARRAY, arrayArgument.handle().kind());
        assertEquals(GpuKernelParameterAccess.READ_ONLY, arrayArgument.access());
        OpenClScalarBinding scalarArgument = assertInstanceOf(OpenClScalarBinding.class, execution.scalarBindings().get(0));
        assertEquals(OpenClArgumentKind.INT32, scalarArgument.kind());
        assertEquals(GpuKernelParameterAccess.VALUE, scalarArgument.access());
    }

    @Test
    void backendCarriesAdditionalPrimitiveKindsIntoPreparedExecution() {
        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "kernel",
                "javatogpu/sample/Demo/kernel.cl",
                "__kernel void kernel() {}",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("bytes", "byte[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("weight", "double", GpuKernelParameterAccess.VALUE),
                        new GpuKernelParameterDescriptor("ints", "int[]", GpuKernelParameterAccess.READ_WRITE)
                )
        );
        AtomicReference<OpenClPreparedExecution> capturedExecution = new AtomicReference<>();

        OpenClGpuRuntimeBackend backend = new OpenClGpuRuntimeBackend() {
            @Override
            protected OpenClCompiledKernel compileKernel(GpuKernelDescriptor kernelDescriptor) {
                return new OpenClCompiledKernel(kernelDescriptor, "compiled:test");
            }

            @Override
            protected void executeKernel(OpenClPreparedExecution execution) {
                capturedExecution.set(execution);
            }
        };

        backend.invoke(new GpuKernelInvocation(descriptor, new Object[]{new byte[]{1, 2}, 1.25d, new int[]{3, 4}}));

        OpenClPreparedExecution execution = capturedExecution.get();
        assertEquals(OpenClArgumentKind.BYTE_ARRAY, execution.bufferBindings().get(0).handle().kind());
        assertEquals(OpenClArgumentKind.INT_ARRAY, execution.bufferBindings().get(1).handle().kind());
        assertEquals(OpenClArgumentKind.FLOAT64, execution.scalarBindings().get(0).kind());
    }
}
