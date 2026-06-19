package net.sixik.ga_utils.javatogpu.runtime.opencl;

import net.sixik.ga_utils.javatogpu.runtime.GpuKernelDescriptor;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterDescriptor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class OpenClExecutionPlannerTest {

    @Test
    void buildsExecutionPlanForBuffersAndScalars() {
        float[] input = new float[]{1.0f, 2.0f};
        float[] output = new float[]{0.0f, 0.0f};
        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "kernel",
                "javatogpu/sample/Demo/kernel.cl",
                "__kernel void kernel() {}",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("input", "float[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("output", "float[]", GpuKernelParameterAccess.READ_WRITE),
                        new GpuKernelParameterDescriptor("count", "int", GpuKernelParameterAccess.VALUE)
                )
        );

        OpenClKernelArguments arguments = OpenClArgumentMarshaller.marshall(descriptor, new Object[]{input, output, 2});
        OpenClExecutionPlan plan = OpenClExecutionPlanner.plan(arguments);

        assertEquals(2, plan.bufferBindings().size());
        assertEquals(1, plan.scalarBindings().size());
        assertEquals(3, plan.argumentBindings().size());
        assertEquals(0, plan.argumentBindings().get(0).parameterIndex());
        assertEquals(1, plan.argumentBindings().get(1).parameterIndex());
        assertEquals(2, plan.argumentBindings().get(2).parameterIndex());
        assertEquals(OpenClArgumentKind.FLOAT_ARRAY, plan.argumentBindings().get(0).bufferBinding().kind());
        assertEquals(OpenClArgumentKind.FLOAT_ARRAY, plan.argumentBindings().get(1).bufferBinding().kind());
        assertEquals(OpenClArgumentKind.INT32, plan.argumentBindings().get(2).scalarBinding().kind());

        OpenClBufferBinding inputBinding = plan.bufferBindings().get(0);
        assertEquals(OpenClArgumentKind.FLOAT_ARRAY, inputBinding.kind());
        assertEquals(GpuKernelParameterAccess.READ_ONLY, inputBinding.access());
        assertEquals(true, inputBinding.uploadRequired());
        assertEquals(false, inputBinding.readbackRequired());
        assertSame(input, inputBinding.sourceArray());

        OpenClBufferBinding outputBinding = plan.bufferBindings().get(1);
        assertEquals(GpuKernelParameterAccess.READ_WRITE, outputBinding.access());
        assertEquals(true, outputBinding.uploadRequired());
        assertEquals(true, outputBinding.readbackRequired());
        assertSame(output, outputBinding.sourceArray());

        OpenClScalarBinding scalarBinding = plan.scalarBindings().get(0);
        assertEquals(OpenClArgumentKind.INT32, scalarBinding.kind());
        assertEquals(2, scalarBinding.value());
    }

    @Test
    void buildsExecutionPlanForAdditionalPrimitiveKinds() {
        byte[] bytes = new byte[]{1, 2};
        long[] longs = new long[]{3L, 4L};
        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "kernel",
                "javatogpu/sample/Demo/kernel.cl",
                "__kernel void kernel() {}",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("bytes", "byte[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("scale", "double", GpuKernelParameterAccess.VALUE),
                        new GpuKernelParameterDescriptor("longs", "long[]", GpuKernelParameterAccess.READ_WRITE)
                )
        );

        OpenClExecutionPlan plan = OpenClExecutionPlanner.plan(
                OpenClArgumentMarshaller.marshall(descriptor, new Object[]{bytes, 2.0d, longs})
        );

        assertEquals(OpenClArgumentKind.BYTE_ARRAY, plan.bufferBindings().get(0).kind());
        assertEquals(OpenClArgumentKind.LONG_ARRAY, plan.bufferBindings().get(1).kind());
        assertEquals(OpenClArgumentKind.FLOAT64, plan.scalarBindings().get(0).kind());
    }

    @Test
    void buildsExecutionPlanForLocalArrayArgument() {
        float[] scratch = new float[8];
        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "kernel",
                "javatogpu/sample/Demo/kernel.cl",
                "__kernel void kernel() {}",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("scratch", "float[]", GpuKernelParameterAccess.LOCAL),
                        new GpuKernelParameterDescriptor("output", "float[]", GpuKernelParameterAccess.READ_WRITE)
                )
        );

        OpenClExecutionPlan plan = OpenClExecutionPlanner.plan(
                OpenClArgumentMarshaller.marshall(descriptor, new Object[]{scratch, new float[8]})
        );

        assertEquals(1, plan.localBindings().size());
        assertEquals(1, plan.bufferBindings().size());
        assertEquals(OpenClArgumentKind.FLOAT_ARRAY, plan.localBindings().get(0).kind());
        assertEquals(8L * Float.BYTES, plan.localBindings().get(0).byteSize());
        assertEquals(0, plan.argumentBindings().get(0).parameterIndex());
        assertEquals(OpenClArgumentKind.FLOAT_ARRAY, plan.argumentBindings().get(0).localBinding().kind());
    }
}
