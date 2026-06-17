package net.sixik.ga_utils.javatogpu.runtime.opencl;

import net.sixik.ga_utils.javatogpu.runtime.GpuKernelDescriptor;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterDescriptor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class OpenClExecutionPreparerTest {

    @Test
    void reusesDeviceBufferHandlesForSameArrayIdentity() {
        float[] input = new float[]{1.0f, 2.0f};
        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "kernel",
                "javatogpu/sample/Demo/kernel.cl",
                "__kernel void kernel() {}",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("input", "float[]", GpuKernelParameterAccess.READ_ONLY)
                )
        );
        OpenClCompiledKernel compiledKernel = new OpenClCompiledKernel(descriptor, "cache-key");
        OpenClKernelArguments arguments = OpenClArgumentMarshaller.marshall(descriptor, new Object[]{input});
        OpenClExecutionPlan plan = OpenClExecutionPlanner.plan(arguments);
        OpenClDeviceBufferRegistry registry = new OpenClDeviceBufferRegistry();
        OpenClExecutionPreparer preparer = new OpenClExecutionPreparer(registry);

        OpenClPreparedExecution first = preparer.prepare(compiledKernel, plan);
        OpenClPreparedExecution second = preparer.prepare(compiledKernel, plan);

        assertEquals(1, first.bufferBindings().size());
        assertSame(
                first.bufferBindings().get(0).handle(),
                second.bufferBindings().get(0).handle()
        );
        assertEquals(1, registry.cacheSize());
    }

    @Test
    void carriesScalarBindingsIntoPreparedExecution() {
        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "kernel",
                "javatogpu/sample/Demo/kernel.cl",
                "__kernel void kernel() {}",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("count", "int", GpuKernelParameterAccess.VALUE)
                )
        );
        OpenClCompiledKernel compiledKernel = new OpenClCompiledKernel(descriptor, "cache-key");
        OpenClKernelArguments arguments = OpenClArgumentMarshaller.marshall(descriptor, new Object[]{4});
        OpenClExecutionPlan plan = OpenClExecutionPlanner.plan(arguments);

        OpenClPreparedExecution prepared = new OpenClExecutionPreparer(new OpenClDeviceBufferRegistry())
                .prepare(compiledKernel, plan);

        assertEquals(0, prepared.bufferBindings().size());
        assertEquals(1, prepared.scalarBindings().size());
        assertEquals(4, prepared.scalarBindings().get(0).value());
        assertEquals(1, prepared.argumentBindings().size());
        assertEquals(0, prepared.argumentBindings().get(0).parameterIndex());
        assertEquals(4, prepared.argumentBindings().get(0).scalarBinding().value());
    }

    @Test
    void preservesKernelArgumentOrderAcrossPreparedBindings() {
        float[] input = new float[]{1.0f, 2.0f};
        float[] output = new float[]{0.0f, 0.0f};
        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "kernel",
                "javatogpu/sample/Demo/kernel.cl",
                "__kernel void kernel() {}",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("input", "float[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("scale", "float", GpuKernelParameterAccess.VALUE),
                        new GpuKernelParameterDescriptor("output", "float[]", GpuKernelParameterAccess.READ_WRITE)
                )
        );
        OpenClCompiledKernel compiledKernel = new OpenClCompiledKernel(descriptor, "cache-key");
        OpenClKernelArguments arguments = OpenClArgumentMarshaller.marshall(descriptor, new Object[]{input, 2.5f, output});
        OpenClPreparedExecution prepared = new OpenClExecutionPreparer(new OpenClDeviceBufferRegistry())
                .prepare(compiledKernel, OpenClExecutionPlanner.plan(arguments));

        assertEquals(3, prepared.argumentBindings().size());
        assertEquals(OpenClArgumentKind.FLOAT_ARRAY, prepared.argumentBindings().get(0).bufferBinding().binding().kind());
        assertEquals(OpenClArgumentKind.FLOAT32, prepared.argumentBindings().get(1).scalarBinding().kind());
        assertEquals(OpenClArgumentKind.FLOAT_ARRAY, prepared.argumentBindings().get(2).bufferBinding().binding().kind());
    }
}
