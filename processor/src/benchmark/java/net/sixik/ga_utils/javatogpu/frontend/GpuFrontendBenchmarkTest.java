package net.sixik.ga_utils.javatogpu.frontend;

import net.sixik.ga_utils.javatogpu.benchmark.BenchmarkHarness;
import net.sixik.ga_utils.javatogpu.benchmark.BenchmarkResult;
import net.sixik.ga_utils.javatogpu.frontend.asm.AsmGpuMethod;
import net.sixik.ga_utils.javatogpu.frontend.model.GpuAddressSpace;
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuMethod;
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuParameter;
import net.sixik.ga_utils.javatogpu.frontend.parser.GpuMethodParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class GpuFrontendBenchmarkTest {

    private static final String DEMO_OWNER = "sample/Demo";
    private static final String HELPERS_OWNER = "sample/Helpers";
    private static final String GPU_OWNER = Type.getInternalName(net.sixik.ga_utils.javatogpu.api.GPU.class);

    @Test
    void benchmarksScalarMathSourceFrontendPipeline() {
        GpuFrontendService service = GpuFrontendService.createDefault();
        String methodSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    int id = GPU.get_global_id(0);
                    float value = input[id];
                    for (int i = 0; i < 12; i++) {
                        value = GPU.sin(value) + GPU.cos(value * 0.5f) + GPU.sqrt(value + 1.0f);
                    }
                    output[id] = value;
                }
                """;

        String kernel = service.parseValidateLowerAndEmit(methodSource);
        assertTrue(kernel.contains("sin"));
        assertTrue(kernel.contains("sqrt"));

        BenchmarkResult result = BenchmarkHarness.measure(
                "frontend.scalar-math.parse-validate-lower-emit",
                5,
                20,
                () -> service.parseValidateLowerAndEmit(methodSource).length()
        );

        assertTrue(result.averageNanos() > 0L);
        assertTrue(result.checksum() > 0L);
    }

    @Test
    void benchmarksHelperHeavySourceFrontendPipeline() {
        GpuMethodParser parser = new GpuMethodParser();
        GpuFrontendService service = GpuFrontendService.createDefault();

        String kernelSource = """
                @GPU
                void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                    int id = GPU.get_global_id(0);
                    float value = input[id];
                    for (int i = 0; i < 8; i++) {
                        value = Helpers.square(value) + Helpers.mix(value, input[id], 0.25f);
                    }
                    output[id] = Helpers.bias(value);
                }
                """;
        String squareHelper = """
                @CCode(inline = true)
                float square(float value) {
                    return value * value;
                }
                """;
        String mixHelper = """
                @CCode(inline = true)
                float mix(float left, float right, float t) {
                    return left + (right - left) * t;
                }
                """;
        String biasHelper = """
                @CCode
                float bias(float value) {
                    return value + 2.0f;
                }
                """;

        ParsedGpuMethod kernelMethod = parser.parseMethod(kernelSource, "Demo", "sample.Demo");
        ParsedGpuMethod squareMethod = parser.parseMethod(squareHelper, "Helpers", "sample.Helpers");
        ParsedGpuMethod mixMethod = parser.parseMethod(mixHelper, "Helpers", "sample.Helpers");
        ParsedGpuMethod biasMethod = parser.parseMethod(biasHelper, "Helpers", "sample.Helpers");

        String kernel = service.validateLowerAndEmit(kernelMethod, List.of(squareMethod, mixMethod, biasMethod));
        assertTrue(kernel.contains("jtg_fn_Helpers_square_float"));
        assertTrue(kernel.contains("jtg_fn_Helpers_mix_float_float_float"));
        assertTrue(kernel.contains("jtg_fn_Helpers_bias_float"));

        BenchmarkResult result = BenchmarkHarness.measure(
                "frontend.helper-heavy.validate-lower-emit",
                5,
                20,
                () -> service.validateLowerAndEmit(kernelMethod, List.of(squareMethod, mixMethod, biasMethod)).length()
        );

        assertTrue(result.averageNanos() > 0L);
        assertTrue(result.checksum() > 0L);
    }

    @Test
    void benchmarksAsmGeneratedKernelPipeline() {
        AsmFrontendService service = AsmFrontendService.createDefault();

        AsmGpuMethod helperMethod = asmHelperMethod();
        AsmGpuMethod kernelMethod = asmKernelMethod();
        String emitted = service.validateLowerAndEmitStructured(kernelMethod, List.of(helperMethod));
        assertTrue(emitted.contains("jtg_fn_Helpers_square_float"));

        BenchmarkResult result = BenchmarkHarness.measure(
                "frontend.asm.validate-lower-emit",
                5,
                20,
                () -> service.validateLowerAndEmitStructured(asmKernelMethod(), List.of(asmHelperMethod())).length()
        );

        assertTrue(result.averageNanos() > 0L);
        assertTrue(result.checksum() > 0L);
    }

    private static AsmGpuMethod asmHelperMethod() {
        MethodNode helperMethodNode = methodNode(HELPERS_OWNER, "square", "(F)F", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, mv -> {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.FLOAD, 0);
            mv.visitVarInsn(Opcodes.FLOAD, 0);
            mv.visitInsn(Opcodes.FMUL);
            mv.visitInsn(Opcodes.FRETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        });
        return new AsmGpuMethod(
                HELPERS_OWNER,
                parsedMethod("Helpers", "sample.Helpers", "square", "float", List.of(parameter("value", "float"))),
                helperMethodNode
        );
    }

    private static AsmGpuMethod asmKernelMethod() {
        MethodNode kernelMethodNode = methodNode(DEMO_OWNER, "kernel", "([F[F)V", Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, mv -> {
            mv.visitCode();
            mv.visitInsn(Opcodes.ICONST_0);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, GPU_OWNER, "get_global_id", "(I)I", false);
            mv.visitVarInsn(Opcodes.ISTORE, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitInsn(Opcodes.FALOAD);
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, HELPERS_OWNER, "square", "(F)F", false);
            mv.visitInsn(Opcodes.FASTORE);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        });
        return new AsmGpuMethod(
                DEMO_OWNER,
                parsedMethod("Demo", "sample.Demo", "kernel", "void", List.of(
                        globalArrayParameter("input", "float[]"),
                        globalArrayParameter("output", "float[]")
                )),
                kernelMethodNode
        );
    }

    private static ParsedGpuMethod parsedMethod(
            String ownerSimpleName,
            String ownerQualifiedName,
            String name,
            String returnType,
            List<ParsedGpuParameter> parameters
    ) {
        return new ParsedGpuMethod(
                ownerSimpleName,
                ownerQualifiedName,
                name,
                returnType,
                parameters,
                List.of(),
                null,
                false,
                List.of(),
                "",
                "",
                "",
                false
        );
    }

    private static ParsedGpuParameter parameter(String name, String javaType) {
        return new ParsedGpuParameter(name, javaType, GpuAddressSpace.PRIVATE, false, List.of());
    }

    private static ParsedGpuParameter globalArrayParameter(String name, String javaType) {
        return new ParsedGpuParameter(name, javaType, GpuAddressSpace.GLOBAL, false, List.of());
    }

    private static MethodNode methodNode(
            String ownerInternalName,
            String methodName,
            String descriptor,
            int access,
            MethodBodyWriter bodyWriter
    ) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, ownerInternalName, null, "java/lang/Object", null);
        MethodVisitor methodVisitor = writer.visitMethod(access, methodName, descriptor, null, null);
        bodyWriter.write(methodVisitor);
        writer.visitEnd();

        ClassNode classNode = new ClassNode();
        new ClassReader(writer.toByteArray()).accept(classNode, 0);
        return classNode.methods.stream()
                .filter(method -> method.name.equals(methodName) && method.desc.equals(descriptor))
                .findFirst()
                .orElseThrow();
    }

    @FunctionalInterface
    private interface MethodBodyWriter {
        void write(MethodVisitor methodVisitor);
    }
}
