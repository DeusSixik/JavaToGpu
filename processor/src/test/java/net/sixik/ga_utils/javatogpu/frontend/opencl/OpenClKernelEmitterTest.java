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
    void emitsConstantAndLocalAddressSpaceKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUConstant float[] lookup, @GPULocal float[] scratch, @GPUGlobal float[] output) {
                    int lid = GPU.get_local_id(0);
                    scratch[lid] = lookup[lid] * 2.0f;
                    output[lid] = scratch[lid];
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__constant float* lookup, __local float* scratch, __global float* output) {
                    int lid = get_local_id(0);
                    scratch[lid] = (lookup[lid] * 2.0f);
                    output[lid] = scratch[lid];
                }""", kernel);
    }

    @Test
    void emitsVectorKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    int id = GPU.get_global_id(0);
                    Float2 value = new Float2(input[id], input[id] * 2.0f);
                    output[id] = value.x + value.y;
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    int id = get_global_id(0);
                    float2 value = (float2)(input[id], (input[id] * 2.0f));
                    output[id] = (value.x + value.y);
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
    void emitsPrimitiveCastKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] output) {
                    int i = 1;
                    output[0] = GPU.sin((float) i);
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global float* output) {
                    int i = 1;
                    output[0] = sin(((float) i));
                }""", kernel);
    }

    @Test
    void emitsBooleanLocalKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    boolean enabled = input[0] > 0.0f;
                    if (enabled) {
                        output[0] = 1.0f;
                    }
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    bool enabled = (input[0] > 0.0f);
                    if (enabled) {
                        output[0] = 1.0f;
                    }
                }""", kernel);
    }

    @Test
    void emitsDoubleMathKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal double[] input, @GPUGlobal double[] output) {
                    int id = GPU.get_global_id(0);
                    double value = GPU.sqrt(input[id]) + GPU.pow(input[id], 2.0);
                    output[id] = GPU.max(value, GPU.log(input[id]));
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global double* input, __global double* output) {
                    int id = get_global_id(0);
                    double value = (sqrt(input[id]) + pow(input[id], 2.0));
                    output[id] = max(value, log(input[id]));
                }""", kernel);
    }

    @Test
    void emitsTemplateBasedIntrinsicKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    int id = GPU.get_global_id(0);
                    output[id] = GPU.fract(input[id] * 1.5f);
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    int id = get_global_id(0);
                    output[id] = (((input[id] * 1.5f)) - floor((input[id] * 1.5f)));
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

    @Test
    void emitsCompoundAssignmentsAndDecrementKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int[] input, @GPUGlobal int[] output) {
                    int id = GPU.get_global_id(0);
                    int value = input[id];
                    value += 2;
                    value <<= 1;
                    for (int i = 3; i > 0; i--) {
                        output[id] += i;
                    }
                    value--;
                    output[id] = value;
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global int* input, __global int* output) {
                    int id = get_global_id(0);
                    int value = input[id];
                    value = (value + 2);
                    value = (value << 1);
                    for (int i = 3; (i > 0); i = (i - 1)) {
                        output[id] = (output[id] + i);
                    }
                    value = (value - 1);
                    output[id] = value;
                }""", kernel);
    }

    @Test
    void emitsWhileDoWhileAndSwitchKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int[] input, @GPUGlobal int[] output) {
                    int i = 0;
                    while (i < 4) {
                        if ((i % 2) == 0) {
                            i++;
                            continue;
                        }
                        output[i] = input[i];
                        i++;
                    }
                    do {
                        i--;
                    } while (i > 0);
                    switch (input[0] & 3) {
                        case 0:
                            output[0] = 1;
                            break;
                        case 1:
                        case 2:
                            output[0] = 2;
                            break;
                        default:
                            output[0] = 3;
                    }
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global int* input, __global int* output) {
                    int i = 0;
                    while ((i < 4)) {
                        if (((i % 2) == 0)) {
                            i = (i + 1);
                            continue;
                        }
                        output[i] = input[i];
                        i = (i + 1);
                    }
                    do {
                        i = (i - 1);
                    } while ((i > 0));
                    switch ((input[0] & 3)) {
                        case 0:
                            output[0] = 1;
                            break;
                        case 1:
                        case 2:
                            output[0] = 2;
                            break;
                        default:
                            output[0] = 3;
                    }
                }""", kernel);
    }

    @Test
    void emitsRuleStyleSwitchKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int[] input, @GPUGlobal int[] output) {
                    switch (input[0] & 3) {
                        case 0 -> output[0] = 1;
                        case 1 -> {
                            output[0] = 2;
                        }
                        default -> output[0] = 3;
                    }
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global int* input, __global int* output) {
                    switch ((input[0] & 3)) {
                        case 0:
                            output[0] = 1;
                            break;
                        case 1:
                            output[0] = 2;
                            break;
                        default:
                            output[0] = 3;
                            break;
                    }
                }""", kernel);
    }

    @Test
    void emitsNanAndSaturatingConversionKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    int id = GPU.get_global_id(0);
                    short narrow = GPU.convert_short_sat(input[id] * 1000.0f);
                    UShort wide = GPU.convert_ushort_sat(input[id] * 1000.0f);
                    output[id] = GPU.nan(id) + narrow + wide.value;
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    int id = get_global_id(0);
                    short narrow = convert_short_sat((input[id] * 1000.0f));
                    ushort wide = convert_ushort_sat((input[id] * 1000.0f));
                    output[id] = ((nan(((uint) (id))) + narrow) + wide);
                }""", kernel);
    }

    @Test
    void emitsWideSaturatingConversionKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal double[] input, @GPUGlobal long[] output) {
                    int id = GPU.get_global_id(0);
                    int signedValue = GPU.convert_int_sat(input[id]);
                    UInt unsignedValue = GPU.convert_uint_sat(input[id]);
                    long wideSigned = GPU.convert_long_sat(input[id]);
                    ULong wideUnsigned = GPU.convert_ulong_sat(input[id]);
                    output[id] = wideSigned + signedValue + unsignedValue.value + wideUnsigned.value;
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global double* input, __global long* output) {
                    int id = get_global_id(0);
                    int signedValue = convert_int_sat(input[id]);
                    uint unsignedValue = convert_uint_sat(input[id]);
                    long wideSigned = convert_long_sat(input[id]);
                    ulong wideUnsigned = convert_ulong_sat(input[id]);
                    output[id] = (((wideSigned + signedValue) + unsignedValue) + wideUnsigned);
                }""", kernel);
    }

    @Test
    void emitsRegularNarrowConversionKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal double[] input, @GPUGlobal int[] output) {
                    int id = GPU.get_global_id(0);
                    byte a = GPU.convert_char(input[id]);
                    UByte b = GPU.convert_uchar(input[id]);
                    short c = GPU.convert_short(input[id]);
                    UShort d = GPU.convert_ushort(input[id]);
                    output[id] = a + b.value + c + d.value;
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global double* input, __global int* output) {
                    int id = get_global_id(0);
                    char a = convert_char(input[id]);
                    uchar b = convert_uchar(input[id]);
                    short c = convert_short(input[id]);
                    ushort d = convert_ushort(input[id]);
                    output[id] = (((a + b) + c) + d);
                }""", kernel);
    }

    @Test
    void emitsUnsignedAliasConversionKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal long[] output) {
                    int id = GPU.get_global_id(0);
                    UByte a = new UByte((byte) 7);
                    UShort b = new UShort((short) 9);
                    UInt c = new UInt(11);
                    ULong d = new ULong(13L);
                    UInt c2 = GPU.convert_uint(a);
                    ULong d2 = GPU.convert_ulong(b);
                    output[id] = GPU.convert_int(c) + GPU.convert_int(d) + GPU.convert_long(c2) + GPU.convert_long(d2) + GPU.convert_int(GPU.convert_float(b)) + GPU.convert_int(GPU.convert_double(a));
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global long* output) {
                    int id = get_global_id(0);
                    uchar a = ((uchar) ((char) 7));
                    ushort b = ((ushort) ((short) 9));
                    uint c = ((uint) 11);
                    ulong d = ((ulong) 13L);
                    uint c2 = ((uint) (a));
                    ulong d2 = ((ulong) (b));
                    output[id] = (((((convert_int(c) + convert_int(d)) + convert_long(c2)) + convert_long(d2)) + convert_int(convert_float(b))) + convert_int(convert_double(a)));
                }""", kernel);
    }

    @Test
    void emitsAdditionalIntegerCommonKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int[] input, @GPUGlobal long[] output) {
                    int id = GPU.get_global_id(0);
                    int a = GPU.hadd(input[id], 3);
                    int b = GPU.mul_hi(input[id], 5);
                    long c = GPU.rhadd((long) input[id], 7L);
                    long d = GPU.mad_hi((long) input[id], 9L, 11L);
                    output[id] = a + b + c + d;
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global int* input, __global long* output) {
                    int id = get_global_id(0);
                    int a = hadd(input[id], 3);
                    int b = mul_hi(input[id], 5);
                    long c = rhadd(((long) input[id]), 7L);
                    long d = mad_hi(((long) input[id]), 9L, 11L);
                    output[id] = (((a + b) + c) + d);
                }""", kernel);
    }

    @Test
    void emitsSaturatingArithmeticKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int[] input, @GPUGlobal long[] output) {
                    int id = GPU.get_global_id(0);
                    int a = GPU.add_sat(input[id], 100);
                    int b = GPU.mad_sat(input[id], 3, 7);
                    long c = GPU.sub_sat((long) input[id], 9L);
                    output[id] = a + b + c;
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global int* input, __global long* output) {
                    int id = get_global_id(0);
                    int a = add_sat(input[id], 100);
                    int b = mad_sat(input[id], 3, 7);
                    long c = sub_sat(((long) input[id]), 9L);
                    output[id] = ((a + b) + c);
                }""", kernel);
    }

    @Test
    void emitsUnsignedSaturatingArithmeticKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal long[] output) {
                    int id = GPU.get_global_id(0);
                    UByte a = new UByte((byte) 10);
                    UShort b = new UShort((short) 20);
                    UInt c = new UInt(30);
                    ULong d = new ULong(40L);
                    UByte x = GPU.add_sat(a, new UByte((byte) 11));
                    UShort y = GPU.mad_sat(b, new UShort((short) 2), new UShort((short) 3));
                    UInt z = GPU.sub_sat(c, new UInt(5));
                    ULong w = GPU.add_sat(d, new ULong(6L));
                    output[id] = x.value + y.value + z.value + w.value;
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global long* output) {
                    int id = get_global_id(0);
                    uchar a = ((uchar) ((char) 10));
                    ushort b = ((ushort) ((short) 20));
                    uint c = ((uint) 30);
                    ulong d = ((ulong) 40L);
                    uchar x = add_sat(a, ((uchar) ((char) 11)));
                    ushort y = mad_sat(b, ((ushort) ((short) 2)), ((ushort) ((short) 3)));
                    uint z = sub_sat(c, ((uint) 5));
                    ulong w = add_sat(d, ((ulong) 6L));
                    output[id] = (((x + y) + z) + w);
                }""", kernel);
    }

    @Test
    void emitsMulSatKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int[] input, @GPUGlobal long[] output) {
                    int id = GPU.get_global_id(0);
                    int a = GPU.mul_sat(input[id], 13);
                    UInt b = GPU.mul_sat(new UInt(7), new UInt(8));
                    output[id] = a + b.value;
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global int* input, __global long* output) {
                    int id = get_global_id(0);
                    int a = mul_sat(input[id], 13);
                    uint b = mul_sat(((uint) 7), ((uint) 8));
                    output[id] = (a + b);
                }""", kernel);
    }

    @Test
    void emitsUnsignedAbsDiffKernel() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal long[] output) {
                    int id = GPU.get_global_id(0);
                    UByte a = GPU.abs_diff(new UByte((byte) 10), new UByte((byte) 3));
                    UShort b = GPU.abs_diff(new UShort((short) 20), new UShort((short) 4));
                    UInt c = GPU.abs_diff(new UInt(30), new UInt(5));
                    ULong d = GPU.abs_diff(new ULong(40L), new ULong(6L));
                    output[id] = a.value + b.value + c.value + d.value;
                }
                """;

        ParsedGpuMethod method = new GpuMethodParser().parseMethod(methodSource);
        GpuIrMethod irMethod = new GpuIrLowerer(GpuIntrinsicDatabase.createDefault()).lower(method);
        String kernel = new OpenClKernelEmitter().emit(method, irMethod);

        assertEquals("""
                __kernel void jtg_kernel(__global long* output) {
                    int id = get_global_id(0);
                    uchar a = abs_diff(((uchar) ((char) 10)), ((uchar) ((char) 3)));
                    ushort b = abs_diff(((ushort) ((short) 20)), ((ushort) ((short) 4)));
                    uint c = abs_diff(((uint) 30), ((uint) 5));
                    ulong d = abs_diff(((ulong) 40L), ((ulong) 6L));
                    output[id] = (((a + b) + c) + d);
                }""", kernel);
    }
}
