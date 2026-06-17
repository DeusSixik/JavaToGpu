package net.sixik.ga_utils.javatogpu.runtime.opencl;

import dev.denismasterherobrine.packager.opencl.core.OpenClException;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelDescriptor;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelInvocation;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterDescriptor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class OpenClGpuRuntimeBackendIntegrationTest {

    @Test
    void runsSimpleKernelOnAvailableOpenClDevice() {
        assumeOpenClAvailable();

        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "gpu_entry",
                "inline://integration/kernel.cl",
                """
                        __kernel void gpu_entry(__global const float* input, float scale, __global float* output) {
                            int id = get_global_id(0);
                            output[id] = input[id] + scale;
                        }""",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("input", "float[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("scale", "float", GpuKernelParameterAccess.VALUE),
                        new GpuKernelParameterDescriptor("output", "float[]", GpuKernelParameterAccess.READ_WRITE)
                )
        );
        float[] input = new float[]{1.0f, 2.0f, 3.0f, 4.0f};
        float[] output = new float[]{0.0f, 0.0f, 0.0f, 0.0f};

        try (OpenClGpuRuntimeBackend backend = new OpenClGpuRuntimeBackend()) {
            backend.invoke(new GpuKernelInvocation(descriptor, new Object[]{input, 2.5f, output}));
        }

        assertArrayEquals(new float[]{3.5f, 4.5f, 5.5f, 6.5f}, output);
    }

    @Test
    void runsLongKernelOnAvailableOpenClDevice() {
        assumeOpenClAvailable();

        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "gpu_long_entry",
                "inline://integration/long-kernel.cl",
                """
                        __kernel void gpu_long_entry(__global const long* input, long offset, __global long* output) {
                            int id = get_global_id(0);
                            output[id] = input[id] + offset;
                        }""",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("input", "long[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("offset", "long", GpuKernelParameterAccess.VALUE),
                        new GpuKernelParameterDescriptor("output", "long[]", GpuKernelParameterAccess.READ_WRITE)
                )
        );
        long[] input = new long[]{10L, 20L, 30L, 40L};
        long[] output = new long[]{0L, 0L, 0L, 0L};

        try (OpenClGpuRuntimeBackend backend = new OpenClGpuRuntimeBackend()) {
            backend.invoke(new GpuKernelInvocation(descriptor, new Object[]{input, 5L, output}));
        }

        assertArrayEquals(new long[]{15L, 25L, 35L, 45L}, output);
    }

    @Test
    void runsDoubleKernelWhenDeviceSupportsFp64() {
        assumeOpenClAvailable();

        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "gpu_double_entry",
                "inline://integration/double-kernel.cl",
                """
                        #pragma OPENCL EXTENSION cl_khr_fp64 : enable
                        __kernel void gpu_double_entry(__global const double* input, double scale, __global double* output) {
                            int id = get_global_id(0);
                            output[id] = input[id] * scale;
                        }""",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("input", "double[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("scale", "double", GpuKernelParameterAccess.VALUE),
                        new GpuKernelParameterDescriptor("output", "double[]", GpuKernelParameterAccess.READ_WRITE)
                )
        );
        assumeKernelCompiles(descriptor, "Skipping fp64 integration smoke test");

        double[] input = new double[]{1.5d, 2.5d, 3.5d};
        double[] output = new double[]{0.0d, 0.0d, 0.0d};

        try (OpenClGpuRuntimeBackend backend = new OpenClGpuRuntimeBackend()) {
            backend.invoke(new GpuKernelInvocation(descriptor, new Object[]{input, 2.0d, output}));
        }

        assertArrayEquals(new double[]{3.0d, 5.0d, 7.0d}, output);
    }

    @Test
    void runsBitwiseIntKernelOnAvailableOpenClDevice() {
        assumeOpenClAvailable();

        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "gpu_bitwise_entry",
                "inline://integration/bitwise-kernel.cl",
                """
                        __kernel void gpu_bitwise_entry(__global const int* input, __global int* output) {
                            int id = get_global_id(0);
                            output[id] = ((~input[id]) << 1) ^ ((input[id] >> 1) | (input[id] & 7));
                        }""",
                java.util.List.of(
                        new GpuKernelParameterDescriptor("input", "int[]", GpuKernelParameterAccess.READ_ONLY),
                        new GpuKernelParameterDescriptor("output", "int[]", GpuKernelParameterAccess.READ_WRITE)
                )
        );
        int[] input = new int[]{1, 2, 7, 16};
        int[] output = new int[]{0, 0, 0, 0};

        try (OpenClGpuRuntimeBackend backend = new OpenClGpuRuntimeBackend()) {
            backend.invoke(new GpuKernelInvocation(descriptor, new Object[]{input, output}));
        }

        assertArrayEquals(
                new int[]{
                        ((~1) << 1) ^ ((1 >> 1) | (1 & 7)),
                        ((~2) << 1) ^ ((2 >> 1) | (2 & 7)),
                        ((~7) << 1) ^ ((7 >> 1) | (7 & 7)),
                        ((~16) << 1) ^ ((16 >> 1) | (16 & 7))
                },
                output
        );
    }

    private static void assumeOpenClAvailable() {
        try (OpenClRuntimeSession ignored = OpenClRuntimeSession.createDefault()) {
            // Session creation is enough for this smoke test to know OpenCL is reachable.
        } catch (UnsatisfiedLinkError | IllegalStateException exception) {
            Assumptions.assumeTrue(false, "Skipping OpenCL integration smoke test: " + exception.getMessage());
        }
    }

    private static void assumeKernelCompiles(GpuKernelDescriptor descriptor, String messagePrefix) {
        try (OpenClRuntimeSession session = OpenClRuntimeSession.createDefault();
             OpenClCompiledKernel ignored = session.compileKernel(descriptor)) {
            // Compilation succeeded, so the runtime test can proceed.
        } catch (UnsatisfiedLinkError | IllegalStateException | OpenClException exception) {
            Assumptions.assumeTrue(false, messagePrefix + ": " + exception.getMessage());
        }
    }
}
