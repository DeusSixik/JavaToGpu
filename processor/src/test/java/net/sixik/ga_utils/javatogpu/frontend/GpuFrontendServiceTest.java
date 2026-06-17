package net.sixik.ga_utils.javatogpu.frontend;

import net.sixik.ga_utils.javatogpu.frontend.ir.model.GpuIrMethod;
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuMethod;
import net.sixik.ga_utils.javatogpu.frontend.validation.GpuValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GpuFrontendServiceTest {

    @Test
    void parsesAndValidatesGpuMethodInOneCall() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    for (int i = 0; i < 4; i++) {
                        output[i] = GPU.sin(input[i]);
                    }
                }
                """;

        GpuFrontendService service = GpuFrontendService.createDefault();
        ParsedGpuMethod method = service.parseAndValidate(methodSource);

        assertEquals("kernel", method.name());
    }

    @Test
    void rejectsMethodsOutsideCurrentGpuSubsetDuringValidation() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] output) {
                    int i = 0;
                    output[i] = GPU.sin((float) i);
                }
                """;

        GpuFrontendService service = GpuFrontendService.createDefault();

        assertThrows(GpuValidationException.class, () -> service.parseAndValidate(methodSource));
    }

    @Test
    void parsesValidatesAndLowersGpuMethodInOneCall() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    int id = GPU.get_global_id(0);
                    float value = input[id];
                    output[id] = GPU.sin(value) * GPU.cos(value);
                }
                """;

        GpuFrontendService service = GpuFrontendService.createDefault();
        GpuIrMethod method = service.parseValidateAndLower(methodSource);

        assertEquals("kernel", method.name());
        assertEquals(3, method.statements().size());
    }

    @Test
    void parsesValidatesLowersAndEmitsGpuMethodInOneCall() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    for (int i = 0; i < 4; i++) {
                        output[i] = GPU.sin(input[i]);
                    }
                }
                """;

        GpuFrontendService service = GpuFrontendService.createDefault();
        String kernel = service.parseValidateLowerAndEmit(methodSource);

        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    for (int i = 0; (i < 4); i = (i + 1)) {
                        output[i] = sin(input[i]);
                    }
                }""", kernel);
    }

    @Test
    void parsesValidatesLowersAndEmitsFloatLiteralKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] output) {
                    output[0] = GPU.sin((1.0f + 2.0f));
                }
                """;

        GpuFrontendService service = GpuFrontendService.createDefault();
        String kernel = service.parseValidateLowerAndEmit(methodSource);

        assertEquals("""
                __kernel void jtg_kernel(__global float* output) {
                    output[0] = sin((1.0f + 2.0f));
                }""", kernel);
    }

    @Test
    void parsesValidatesLowersAndEmitsIfElseKernel() {
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

        GpuFrontendService service = GpuFrontendService.createDefault();
        String kernel = service.parseValidateLowerAndEmit(methodSource);

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
    void parsesValidatesLowersAndEmitsLogicalConditionKernel() {
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

        GpuFrontendService service = GpuFrontendService.createDefault();
        String kernel = service.parseValidateLowerAndEmit(methodSource);

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
    void parsesValidatesLowersAndEmitsElseIfKernel() {
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

        GpuFrontendService service = GpuFrontendService.createDefault();
        String kernel = service.parseValidateLowerAndEmit(methodSource);

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
    void parsesValidatesLowersAndEmitsTernaryKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    int id = GPU.get_global_id(0);
                    output[id] = input[id] > 0.0f ? GPU.sin(input[id]) : GPU.cos(input[id]);
                }
                """;

        GpuFrontendService service = GpuFrontendService.createDefault();
        String kernel = service.parseValidateLowerAndEmit(methodSource);

        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    int id = get_global_id(0);
                    output[id] = ((input[id] > 0.0f) ? sin(input[id]) : cos(input[id]));
                }""", kernel);
    }

    @Test
    void parsesValidatesLowersAndEmitsDivisionModuloKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int[] input, @GPUGlobal int[] output) {
                    int id = GPU.get_global_id(0);
                    int value = -input[id];
                    output[id] = (value / 2) % 3;
                }
                """;

        GpuFrontendService service = GpuFrontendService.createDefault();
        String kernel = service.parseValidateLowerAndEmit(methodSource);

        assertEquals("""
                __kernel void jtg_kernel(__global int* input, __global int* output) {
                    int id = get_global_id(0);
                    int value = (-input[id]);
                    output[id] = ((value / 2) % 3);
                }""", kernel);
    }

    @Test
    void parsesValidatesLowersAndEmitsBitwiseIntegerKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int[] input, @GPUGlobal int[] output) {
                    int id = GPU.get_global_id(0);
                    output[id] = ((~input[id]) << 1) ^ ((input[id] >> 1) | (input[id] & 7));
                }
                """;

        GpuFrontendService service = GpuFrontendService.createDefault();
        String kernel = service.parseValidateLowerAndEmit(methodSource);

        assertEquals("""
                __kernel void jtg_kernel(__global int* input, __global int* output) {
                    int id = get_global_id(0);
                    output[id] = (((~input[id]) << 1) ^ ((input[id] >> 1) | (input[id] & 7)));
                }""", kernel);
    }

    @Test
    void rejectsNonVoidGpuMethodAtFrontendBoundary() {
        String methodSource = """
                @GPU
                float kernel(@GPUGlobal float[] input) {
                    return input[0];
                }
                """;

        GpuFrontendService service = GpuFrontendService.createDefault();

        assertThrows(GpuValidationException.class, () -> service.parseAndValidate(methodSource));
    }

    @Test
    void rejectsArrayParametersWithoutGlobalAnnotationAtFrontendBoundary() {
        String methodSource = """
                @GPU
                void kernel(float[] input, @GPUGlobal float[] output) {
                    output[0] = input[0];
                }
                """;

        GpuFrontendService service = GpuFrontendService.createDefault();

        assertThrows(GpuValidationException.class, () -> service.parseAndValidate(methodSource));
    }
}
