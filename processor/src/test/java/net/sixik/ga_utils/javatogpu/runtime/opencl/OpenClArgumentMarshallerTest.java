package net.sixik.ga_utils.javatogpu.runtime.opencl;

import net.sixik.ga_utils.javatogpu.runtime.GpuKernelDescriptor;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterDescriptor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class OpenClArgumentMarshallerTest {

    @Test
    void marshalsFloatArrayAndPrimitiveScalarArguments() {
        float[] input = new float[]{1.0f, 2.0f, 3.0f};
        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "kernel",
                "javatogpu/sample/Demo/kernel.cl",
                "__kernel void kernel() {}",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("input", "float[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("count", "int", GpuKernelParameterAccess.VALUE),
                        new GpuKernelParameterDescriptor("scale", "float", GpuKernelParameterAccess.VALUE)
                )
        );
        Object[] arguments = new Object[]{input, 7, 2.5f};

        OpenClKernelArguments marshalled = OpenClArgumentMarshaller.marshall(descriptor, arguments);

        assertEquals(3, marshalled.values().size());

        OpenClArrayArgument arrayArgument = assertInstanceOf(OpenClArrayArgument.class, marshalled.values().get(0));
        assertEquals(OpenClArgumentKind.FLOAT_ARRAY, arrayArgument.kind());
        assertEquals(GpuKernelParameterAccess.READ_ONLY, arrayArgument.access());
        assertSame(input, arrayArgument.sourceArray());
        assertEquals(3, arrayArgument.length());

        OpenClScalarArgument intArgument = assertInstanceOf(OpenClScalarArgument.class, marshalled.values().get(1));
        assertEquals(OpenClArgumentKind.INT32, intArgument.kind());
        assertEquals(GpuKernelParameterAccess.VALUE, intArgument.access());
        assertEquals(7, intArgument.value());

        OpenClScalarArgument floatArgument = assertInstanceOf(OpenClScalarArgument.class, marshalled.values().get(2));
        assertEquals(OpenClArgumentKind.FLOAT32, floatArgument.kind());
        assertEquals(GpuKernelParameterAccess.VALUE, floatArgument.access());
        assertEquals(2.5f, floatArgument.value());
    }

    @Test
    void marshalsAdditionalPrimitiveArraysAndScalars() {
        byte[] bytes = new byte[]{1, 2};
        short[] shorts = new short[]{3, 4};
        int[] ints = new int[]{5, 6};
        long[] longs = new long[]{7L, 8L};
        double[] doubles = new double[]{1.5, 2.5};
        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "kernel",
                "javatogpu/sample/Demo/kernel.cl",
                "__kernel void kernel() {}",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("bytes", "byte[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("shorts", "short[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("ints", "int[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("longs", "long[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("doubles", "double[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("byteValue", "byte", GpuKernelParameterAccess.VALUE),
                        new GpuKernelParameterDescriptor("shortValue", "short", GpuKernelParameterAccess.VALUE),
                        new GpuKernelParameterDescriptor("longValue", "long", GpuKernelParameterAccess.VALUE),
                        new GpuKernelParameterDescriptor("doubleValue", "double", GpuKernelParameterAccess.VALUE)
                )
        );

        OpenClKernelArguments marshalled = OpenClArgumentMarshaller.marshall(
                descriptor,
                new Object[]{bytes, shorts, ints, longs, doubles, (byte) 9, (short) 10, 11L, 12.5d}
        );

        assertEquals(OpenClArgumentKind.BYTE_ARRAY, assertInstanceOf(OpenClArrayArgument.class, marshalled.values().get(0)).kind());
        assertEquals(OpenClArgumentKind.SHORT_ARRAY, assertInstanceOf(OpenClArrayArgument.class, marshalled.values().get(1)).kind());
        assertEquals(OpenClArgumentKind.INT_ARRAY, assertInstanceOf(OpenClArrayArgument.class, marshalled.values().get(2)).kind());
        assertEquals(OpenClArgumentKind.LONG_ARRAY, assertInstanceOf(OpenClArrayArgument.class, marshalled.values().get(3)).kind());
        assertEquals(OpenClArgumentKind.DOUBLE_ARRAY, assertInstanceOf(OpenClArrayArgument.class, marshalled.values().get(4)).kind());
        assertEquals(OpenClArgumentKind.INT8, assertInstanceOf(OpenClScalarArgument.class, marshalled.values().get(5)).kind());
        assertEquals(OpenClArgumentKind.INT16, assertInstanceOf(OpenClScalarArgument.class, marshalled.values().get(6)).kind());
        assertEquals(OpenClArgumentKind.INT64, assertInstanceOf(OpenClScalarArgument.class, marshalled.values().get(7)).kind());
        assertEquals(OpenClArgumentKind.FLOAT64, assertInstanceOf(OpenClScalarArgument.class, marshalled.values().get(8)).kind());
    }
}
