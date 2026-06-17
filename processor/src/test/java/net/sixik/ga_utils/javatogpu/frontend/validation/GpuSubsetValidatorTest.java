package net.sixik.ga_utils.javatogpu.frontend.validation;

import net.sixik.ga_utils.javatogpu.frontend.intrinsics.GpuIntrinsicDatabase;
import net.sixik.ga_utils.javatogpu.frontend.parser.GpuMethodParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GpuSubsetValidatorTest {

    private final GpuMethodParser parser = new GpuMethodParser();
    private final GpuSubsetValidator validator = new GpuSubsetValidator(GpuIntrinsicDatabase.createDefault());

    @Test
    void acceptsPrimitiveArraysForLoopsAndGpuIntrinsics() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    for (int i = 0; i < 16; i++) {
                        float value = input[i];
                        output[i] = GPU.sin(value) + GPU.cos(value);
                    }
                }
                """;

        assertDoesNotThrow(() -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void rejectsObjectAllocation() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] output) {
                    Object value = new Object();
                    output[0] = 1.0f;
                }
                """;

        assertThrows(GpuValidationException.class, () -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void rejectsExternalJavaCalls() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] output) {
                    output[0] = (float) Math.sin(1.0);
                }
                """;

        assertThrows(GpuValidationException.class, () -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void rejectsWhileLoops() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] output) {
                    int i = 0;
                    while (i < 4) {
                        output[i] = 1.0f;
                        i++;
                    }
                }
                """;

        assertThrows(GpuValidationException.class, () -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void rejectsIfBranchesWithoutBraces() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] output) {
                    if (output[0] > 0)
                        output[0] = 1;
                }
                """;

        assertThrows(GpuValidationException.class, () -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void acceptsIfElseBranchesWithBraces() {
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

        assertDoesNotThrow(() -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void acceptsLogicalOperatorsInIfConditions() {
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

        assertDoesNotThrow(() -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void acceptsElseIfChains() {
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

        assertDoesNotThrow(() -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void acceptsTernaryExpressions() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    int id = GPU.get_global_id(0);
                    output[id] = input[id] > 0.0f ? GPU.sin(input[id]) : GPU.cos(input[id]);
                }
                """;

        assertDoesNotThrow(() -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void acceptsDivisionModuloAndUnaryMinusExpressions() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int[] input, @GPUGlobal int[] output) {
                    int id = GPU.get_global_id(0);
                    int value = -input[id];
                    output[id] = (value / 2) % 3;
                }
                """;

        assertDoesNotThrow(() -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void rejectsCastExpressionsBeforeLowering() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] output) {
                    int i = 1;
                    output[0] = GPU.sin((float) i);
                }
                """;

        assertThrows(GpuValidationException.class, () -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void rejectsExpressionStatementsThatAreNotAssignmentsOrDeclarations() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] output) {
                    GPU.sin(output[0]);
                }
                """;

        assertThrows(GpuValidationException.class, () -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void acceptsFloatLiteralsAndParenthesizedExpressions() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] output) {
                    output[0] = GPU.sin((1.0f + 2.0f));
                }
                """;

        assertDoesNotThrow(() -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void rejectsNonVoidGpuMethods() {
        String methodSource = """
                @GPU
                float kernel(@GPUGlobal float[] input) {
                    return input[0];
                }
                """;

        assertThrows(GpuValidationException.class, () -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void rejectsBooleanParametersBecauseRuntimeDoesNotMarshalThem() {
        String methodSource = """
                @GPU
                void kernel(boolean enabled, @GPUGlobal float[] output) {
                    output[0] = 1.0f;
                }
                """;

        assertThrows(GpuValidationException.class, () -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void rejectsGlobalScalarParameters() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int count, @GPUGlobal float[] output) {
                    output[0] = count;
                }
                """;

        assertThrows(GpuValidationException.class, () -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void rejectsArrayParametersWithoutGlobalAnnotation() {
        String methodSource = """
                @GPU
                void kernel(float[] input, @GPUGlobal float[] output) {
                    output[0] = input[0];
                }
                """;

        assertThrows(GpuValidationException.class, () -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void rejectsBooleanLocalsBecauseCurrentEmitterDoesNotModelThem() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    boolean enabled = input[0] > 0.0f;
                    if (enabled) {
                        output[0] = 1.0f;
                    }
                }
                """;

        assertThrows(GpuValidationException.class, () -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void acceptsBitwiseIntegerOperators() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int[] input, @GPUGlobal int[] output) {
                    int id = GPU.get_global_id(0);
                    output[id] = ((~input[id]) << 1) ^ ((input[id] >> 1) | (input[id] & 7));
                }
                """;

        assertDoesNotThrow(() -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void rejectsUnsignedRightShiftOperator() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal int[] input, @GPUGlobal int[] output) {
                    int id = GPU.get_global_id(0);
                    output[id] = input[id] >>> 1;
                }
                """;

        assertThrows(GpuValidationException.class, () -> validator.validate(parser.parseMethod(methodSource)));
    }

    @Test
    void rejectsBitwiseOperatorsForFloatingPointExpressions() {
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    int id = GPU.get_global_id(0);
                    output[id] = input[id] << 1;
                }
                """;

        assertThrows(GpuValidationException.class, () -> validator.validate(parser.parseMethod(methodSource)));
    }
}
