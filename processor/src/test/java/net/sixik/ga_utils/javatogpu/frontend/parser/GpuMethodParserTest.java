package net.sixik.ga_utils.javatogpu.frontend.parser;

import net.sixik.ga_utils.javatogpu.frontend.model.GpuAddressSpace;
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpuMethodParserTest {

    @Test
    void parsesAnnotatedMethodMetadata() {
        String methodSource = """
                @GPU
                void my_gpu_function_math(
                    @GPUGlobal(constant = true) float[] input,
                    @GPUGlobal float[] output
                ) {
                    int id = GPU.get_global_id(0);
                    float value = input[id];
                    output[id] = GPU.sin(value) * GPU.cos(value);
                }
                """;

        GpuMethodParser parser = new GpuMethodParser();
        ParsedGpuMethod method = parser.parseMethod(methodSource);

        assertEquals("my_gpu_function_math", method.name());
        assertEquals("void", method.returnType());
        assertEquals(2, method.parameters().size());
        assertEquals("input", method.parameters().get(0).name());
        assertEquals("float[]", method.parameters().get(0).javaType());
        assertEquals(GpuAddressSpace.GLOBAL, method.parameters().get(0).addressSpace());
        assertTrue(method.parameters().get(0).constant());
    }
}
