package net.sixik.ga_utils.javatogpu.frontend.opencl;

import net.sixik.ga_utils.javatogpu.frontend.intrinsics.GpuIntrinsicDatabase;
import net.sixik.ga_utils.javatogpu.frontend.ir.model.GpuIrMethod;
import net.sixik.ga_utils.javatogpu.frontend.lowering.GpuIrLowerer;
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuMethod;
import net.sixik.ga_utils.javatogpu.frontend.parser.GpuMethodParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenClKernelEmitterTest {

    @Test
    void emitsSimpleMathKernel() {
        String methodSource = """
                @GPU
                void kernel(
                    @GPUGlobal(constant = true) float[] input,
                    @GPUGlobal float[] output
                ) {
                    int id = GPU.get_global_id(0);
                    float value = input[id];
                    output[id] = GPU.sin(value) * GPU.cos(value);
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global const float* input, __global float* output) {
                    int id = get_global_id(0);
                    float value = input[id];
                    output[id] = (sin(value) * cos(value));
                }""", kernel);
    }

    @Test
    void emitsForLoopKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    for (int i = 0; i < 4; i++) {
                        output[i] = GPU.sin(input[i]);
                    }
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    for (int i = 0; (i < 4); i = (i + 1)) {
                        output[i] = sin(input[i]);
                    }
                }""", kernel);
    }

    @Test
    void emitsFloatLiteralKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] output) {
                    output[0] = GPU.sin((1.0f + 2.0f));
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global float* output) {
                    output[0] = sin((1.0f + 2.0f));
                }""", kernel);
    }

    @Test
    void emitsIfElseKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    int id = GPU.get_global_id(0);
                    if (input[id] > 0.0f) {
                        output[id] = GPU.sin(input[id]);
                    } else {
                        output[id] = GPU.cos(input[id]);
                    }
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    int id = get_global_id(0);
                    if ((input[id] > 0.0f)) {
                        output[id] = sin(input[id]);
                    } else {
                        output[id] = cos(input[id]);
                    }
                }""", kernel);
    }

    @Test
    void emitsLogicalConditionKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    int id = GPU.get_global_id(0);
                    if ((input[id] > 0.0f && input[id] < 10.0f) || !(input[id] > 100.0f)) {
                        output[id] = GPU.sin(input[id]);
                    } else {
                        output[id] = GPU.cos(input[id]);
                    }
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    int id = get_global_id(0);
                    if ((((input[id] > 0.0f) && (input[id] < 10.0f)) || (!(input[id] > 100.0f)))) {
                        output[id] = sin(input[id]);
                    } else {
                        output[id] = cos(input[id]);
                    }
                }""", kernel);
    }

    @Test
    void emitsElseIfKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    int id = GPU.get_global_id(0);
                    if (input[id] > 10.0f) {
                        output[id] = GPU.sin(input[id]);
                    } else if (input[id] > 0.0f) {
                        output[id] = GPU.cos(input[id]);
                    } else {
                        output[id] = input[id];
                    }
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    int id = get_global_id(0);
                    if ((input[id] > 10.0f)) {
                        output[id] = sin(input[id]);
                    } else {
                        if ((input[id] > 0.0f)) {
                            output[id] = cos(input[id]);
                        } else {
                            output[id] = input[id];
                        }
                    }
                }""", kernel);
    }

    @Test
    void emitsTernaryKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    int id = GPU.get_global_id(0);
                    output[id] = input[id] > 0.0f ? GPU.sin(input[id]) : GPU.cos(input[id]);
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    int id = get_global_id(0);
                    output[id] = ((input[id] > 0.0f) ? sin(input[id]) : cos(input[id]));
                }""", kernel);
    }

    @Test
    void emitsDivisionModuloAndUnaryMinusKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int[] input, @GPUGlobal int[] output) {
                    int id = GPU.get_global_id(0);
                    int value = -input[id];
                    output[id] = (value / 2) % 3;
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global int* input, __global int* output) {
                    int id = get_global_id(0);
                    int value = (-input[id]);
                    output[id] = ((value / 2) % 3);
                }""", kernel);
    }

    @Test
    void emitsBitwiseIntegerKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int[] input, @GPUGlobal int[] output) {
                    int id = GPU.get_global_id(0);
                    output[id] = ((~input[id]) << 1) ^ ((input[id] >> 1) | (input[id] & 7));
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global int* input, __global int* output) {
                    int id = get_global_id(0);
                    output[id] = (((~input[id]) << 1) ^ ((input[id] >> 1) | (input[id] & 7)));
                }""", kernel);
    }
}
