package net.sixik.ga_utils.javatogpu.processors;

import net.sixik.ga_utils.javatogpu.runtime.GpuKernelDescriptor;
import net.sixik.ga_utils.javatogpu.runtime.GpuGeneratedLauncherInvoker;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelInvocation;
import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess;
import net.sixik.ga_utils.javatogpu.runtime.GpuRuntime;
import net.sixik.ga_utils.javatogpu.runtime.GpuRuntimeBackend;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpuCompilerProcessorTest {

    @Test
    void generatesOpenClKernelFileDuringCompilation() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classOutputDir = Files.createTempDirectory("javatogpu-processor-classes");
        Path generatedOutputDir = Files.createTempDirectory("javatogpu-processor-generated");

        String source = """
                package sample;

                import net.sixik.ga_utils.javatogpu.api.GPU;
                import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;

                public class Demo {
                    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
                    void kernel(@GPUGlobal(constant = true) float[] input, @GPUGlobal float[] output) {
                        int id = GPU.get_global_id(0);
                        float value = input[id];
                        output[id] = GPU.sin(value) * GPU.cos(value);
                    }
                }
                """;

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classOutputDir.toString(),
                    "-s", generatedOutputDir.toString()
            );
            JavaFileObject sourceFile = new StringJavaFileObject("sample.Demo", source);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    null,
                    options,
                    null,
                    List.of(sourceFile)
            );
            task.setProcessors(List.of(new GpuCompilerProcessor()));

            assertTrue(task.call());
        }

        Path kernelPath = generatedOutputDir.resolve("javatogpu/sample/Demo/kernel.cl");
        assertTrue(Files.exists(kernelPath));
        assertEquals("""
                __kernel void jtg_kernel(__global const float* input, __global float* output) {
                    int id = get_global_id(0);
                    float value = input[id];
                    output[id] = (sin(value) * cos(value));
                }""", Files.readString(kernelPath));

        Path launcherSourcePath = generatedOutputDir.resolve("sample/generated/Demo_kernel_GpuLauncher.java");
        assertTrue(Files.exists(launcherSourcePath));
        String launcherSource = Files.readString(launcherSourcePath);
        assertTrue(launcherSource.contains("public final class Demo_kernel_GpuLauncher"));
        assertTrue(launcherSource.contains("public static final String KERNEL_NAME = \"jtg_kernel\";"));
        assertTrue(launcherSource.contains("public static final String KERNEL_RESOURCE = \"javatogpu/sample/Demo/kernel.cl\";"));
        assertTrue(launcherSource.contains("new net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterDescriptor(\"input\", \"float[]\", net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess.READ_ONLY)"));
        assertTrue(launcherSource.contains("new net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterDescriptor(\"output\", \"float[]\", net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess.READ_WRITE)"));
        assertTrue(launcherSource.contains("public static void invoke(float[] input, float[] output)"));

        Path launcherClassPath = classOutputDir.resolve("sample/generated/Demo_kernel_GpuLauncher.class");
        assertTrue(Files.exists(launcherClassPath));

        AtomicReference<GpuKernelInvocation> capturedInvocation = new AtomicReference<>();
        GpuRuntimeBackend previousBackend = GpuRuntime.backend();
        GpuRuntime.setBackend(capturedInvocation::set);

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{classOutputDir.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> launcherClass = Class.forName("sample.generated.Demo_kernel_GpuLauncher", true, classLoader);
            Class<?> ownerClass = Class.forName("sample.Demo", true, classLoader);
            float[] input = new float[]{1.0f, 2.0f};
            float[] output = new float[]{0.0f, 0.0f};
            launcherClass.getMethod("invoke", float[].class, float[].class).invoke(null, input, output);

            GpuKernelInvocation invocation = capturedInvocation.get();
            GpuKernelDescriptor descriptor = invocation.descriptor();
            assertEquals("jtg_kernel", descriptor.kernelName());
            assertEquals("javatogpu/sample/Demo/kernel.cl", descriptor.kernelResource());
            assertEquals(Files.readString(kernelPath), descriptor.kernelSource());
            assertEquals(2, descriptor.parameterDescriptors().size());
            assertEquals(GpuKernelParameterAccess.READ_ONLY, descriptor.parameterDescriptors().get(0).access());
            assertEquals(GpuKernelParameterAccess.READ_WRITE, descriptor.parameterDescriptors().get(1).access());
            assertTrue(Arrays.equals(new Object[]{input, output}, invocation.arguments()));

            capturedInvocation.set(null);
            GpuGeneratedLauncherInvoker.invoke(ownerClass, "kernel", input, output);
            GpuKernelInvocation reflectedInvocation = capturedInvocation.get();
            assertEquals("jtg_kernel", reflectedInvocation.descriptor().kernelName());
            assertTrue(Arrays.equals(new Object[]{input, output}, reflectedInvocation.arguments()));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to invoke generated launcher reflectively", exception);
        } finally {
            GpuRuntime.setBackend(previousBackend);
        }
    }

    @Test
    void generatesLauncherForNestedGpuOwnerWithoutInvalidPackage() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classOutputDir = Files.createTempDirectory("javatogpu-nested-classes");
        Path generatedOutputDir = Files.createTempDirectory("javatogpu-nested-generated");

        String source = """
                package sample;

                import net.sixik.ga_utils.javatogpu.api.GPU;
                import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;

                public class Outer {
                    static class Inner {
                        @net.sixik.ga_utils.javatogpu.api.anotations.GPU
                        static void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                            output[0] = GPU.sin(input[0]);
                        }
                    }
                }
                """;

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classOutputDir.toString(),
                    "-s", generatedOutputDir.toString()
            );
            JavaFileObject sourceFile = new StringJavaFileObject("sample.Outer", source);
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, options, null, List.of(sourceFile));
            task.setProcessors(List.of(new GpuCompilerProcessor()));

            assertTrue(task.call());
        }

        Path launcherSourcePath = generatedOutputDir.resolve("sample/generated/Outer_Inner_kernel_GpuLauncher.java");
        assertTrue(Files.exists(launcherSourcePath));

        AtomicReference<GpuKernelInvocation> capturedInvocation = new AtomicReference<>();
        GpuRuntimeBackend previousBackend = GpuRuntime.backend();
        GpuRuntime.setBackend(capturedInvocation::set);

        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{classOutputDir.toUri().toURL()}, getClass().getClassLoader())) {
            Class<?> innerClass = Class.forName("sample.Outer$Inner", true, classLoader);
            float[] input = new float[]{1.0f};
            float[] output = new float[]{0.0f};

            GpuGeneratedLauncherInvoker.invoke(innerClass, "kernel", input, output);

            GpuKernelInvocation invocation = capturedInvocation.get();
            assertEquals("jtg_kernel", invocation.descriptor().kernelName());
            assertTrue(Arrays.equals(new Object[]{input, output}, invocation.arguments()));
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to invoke generated nested launcher reflectively", exception);
        } finally {
            GpuRuntime.setBackend(previousBackend);
        }
    }

    @Test
    void rejectsNonVoidGpuMethodDuringCompilation() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classOutputDir = Files.createTempDirectory("javatogpu-nonvoid-classes");
        Path generatedOutputDir = Files.createTempDirectory("javatogpu-nonvoid-generated");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        String source = """
                package sample;

                import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;

                public class Demo {
                    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
                    float kernel(@GPUGlobal float[] input) {
                        return input[0];
                    }
                }
                """;

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classOutputDir.toString(),
                    "-s", generatedOutputDir.toString()
            );
            JavaFileObject sourceFile = new StringJavaFileObject("sample.Demo", source);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    options,
                    null,
                    List.of(sourceFile)
            );
            task.setProcessors(List.of(new GpuCompilerProcessor()));

            assertFalse(task.call());
        }

        assertTrue(
                diagnostics.getDiagnostics().stream().anyMatch(diagnostic ->
                        String.valueOf(diagnostic.getMessage(null)).contains(
                                "Non-void @GPU methods are not supported in the current pipeline; use a void method with output buffers"
                        )
                )
        );
    }

    @Test
    void generatesKernelWithIfElseBranching() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classOutputDir = Files.createTempDirectory("javatogpu-if-classes");
        Path generatedOutputDir = Files.createTempDirectory("javatogpu-if-generated");

        String source = """
                package sample;

                import net.sixik.ga_utils.javatogpu.api.GPU;
                import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;

                public class Demo {
                    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
                    void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                        int id = GPU.get_global_id(0);
                        if (input[id] > 0.0f) {
                            output[id] = GPU.sin(input[id]);
                        } else {
                            output[id] = GPU.cos(input[id]);
                        }
                    }
                }
                """;

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classOutputDir.toString(),
                    "-s", generatedOutputDir.toString()
            );
            JavaFileObject sourceFile = new StringJavaFileObject("sample.Demo", source);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    null,
                    options,
                    null,
                    List.of(sourceFile)
            );
            task.setProcessors(List.of(new GpuCompilerProcessor()));

            assertTrue(task.call());
        }

        Path kernelPath = generatedOutputDir.resolve("javatogpu/sample/Demo/kernel.cl");
        assertTrue(Files.exists(kernelPath));
        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    int id = get_global_id(0);
                    if ((input[id] > 0.0F)) {
                        output[id] = sin(input[id]);
                    } else {
                        output[id] = cos(input[id]);
                    }
                }""", Files.readString(kernelPath));
    }

    @Test
    void generatesKernelWithLogicalConditionBranching() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classOutputDir = Files.createTempDirectory("javatogpu-logical-classes");
        Path generatedOutputDir = Files.createTempDirectory("javatogpu-logical-generated");

        String source = """
                package sample;

                import net.sixik.ga_utils.javatogpu.api.GPU;
                import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;

                public class Demo {
                    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
                    void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                        int id = GPU.get_global_id(0);
                        if ((input[id] > 0.0f && input[id] < 10.0f) || !(input[id] > 100.0f)) {
                            output[id] = GPU.sin(input[id]);
                        } else {
                            output[id] = GPU.cos(input[id]);
                        }
                    }
                }
                """;

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classOutputDir.toString(),
                    "-s", generatedOutputDir.toString()
            );
            JavaFileObject sourceFile = new StringJavaFileObject("sample.Demo", source);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    null,
                    options,
                    null,
                    List.of(sourceFile)
            );
            task.setProcessors(List.of(new GpuCompilerProcessor()));

            assertTrue(task.call());
        }

        Path kernelPath = generatedOutputDir.resolve("javatogpu/sample/Demo/kernel.cl");
        assertTrue(Files.exists(kernelPath));
        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    int id = get_global_id(0);
                    if ((((input[id] > 0.0F) && (input[id] < 10.0F)) || (!(input[id] > 100.0F)))) {
                        output[id] = sin(input[id]);
                    } else {
                        output[id] = cos(input[id]);
                    }
                }""", Files.readString(kernelPath));
    }

    @Test
    void generatesKernelWithElseIfBranching() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classOutputDir = Files.createTempDirectory("javatogpu-elseif-classes");
        Path generatedOutputDir = Files.createTempDirectory("javatogpu-elseif-generated");

        String source = """
                package sample;

                import net.sixik.ga_utils.javatogpu.api.GPU;
                import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;

                public class Demo {
                    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
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
                }
                """;

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classOutputDir.toString(),
                    "-s", generatedOutputDir.toString()
            );
            JavaFileObject sourceFile = new StringJavaFileObject("sample.Demo", source);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    null,
                    options,
                    null,
                    List.of(sourceFile)
            );
            task.setProcessors(List.of(new GpuCompilerProcessor()));

            assertTrue(task.call());
        }

        Path kernelPath = generatedOutputDir.resolve("javatogpu/sample/Demo/kernel.cl");
        assertTrue(Files.exists(kernelPath));
        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    int id = get_global_id(0);
                    if ((input[id] > 10.0F)) {
                        output[id] = sin(input[id]);
                    } else {
                        if ((input[id] > 0.0F)) {
                            output[id] = cos(input[id]);
                        } else {
                            output[id] = input[id];
                        }
                    }
                }""", Files.readString(kernelPath));
    }

    @Test
    void generatesKernelWithTernaryExpression() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classOutputDir = Files.createTempDirectory("javatogpu-ternary-classes");
        Path generatedOutputDir = Files.createTempDirectory("javatogpu-ternary-generated");

        String source = """
                package sample;

                import net.sixik.ga_utils.javatogpu.api.GPU;
                import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;

                public class Demo {
                    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
                    void kernel(@GPUGlobal float[] input, @GPUGlobal float[] output) {
                        int id = GPU.get_global_id(0);
                        output[id] = input[id] > 0.0f ? GPU.sin(input[id]) : GPU.cos(input[id]);
                    }
                }
                """;

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classOutputDir.toString(),
                    "-s", generatedOutputDir.toString()
            );
            JavaFileObject sourceFile = new StringJavaFileObject("sample.Demo", source);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    null,
                    options,
                    null,
                    List.of(sourceFile)
            );
            task.setProcessors(List.of(new GpuCompilerProcessor()));

            assertTrue(task.call());
        }

        Path kernelPath = generatedOutputDir.resolve("javatogpu/sample/Demo/kernel.cl");
        assertTrue(Files.exists(kernelPath));
        assertEquals("""
                __kernel void jtg_kernel(__global float* input, __global float* output) {
                    int id = get_global_id(0);
                    output[id] = ((input[id] > 0.0F) ? sin(input[id]) : cos(input[id]));
                }""", Files.readString(kernelPath));
    }

    @Test
    void generatesKernelWithDivisionModuloAndUnaryMinus() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classOutputDir = Files.createTempDirectory("javatogpu-arith-classes");
        Path generatedOutputDir = Files.createTempDirectory("javatogpu-arith-generated");

        String source = """
                package sample;

                import net.sixik.ga_utils.javatogpu.api.GPU;
                import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;

                public class Demo {
                    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
                    void kernel(@GPUGlobal int[] input, @GPUGlobal int[] output) {
                        int id = GPU.get_global_id(0);
                        int value = -input[id];
                        output[id] = (value / 2) % 3;
                    }
                }
                """;

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classOutputDir.toString(),
                    "-s", generatedOutputDir.toString()
            );
            JavaFileObject sourceFile = new StringJavaFileObject("sample.Demo", source);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    null,
                    options,
                    null,
                    List.of(sourceFile)
            );
            task.setProcessors(List.of(new GpuCompilerProcessor()));

            assertTrue(task.call());
        }

        Path kernelPath = generatedOutputDir.resolve("javatogpu/sample/Demo/kernel.cl");
        assertTrue(Files.exists(kernelPath));
        assertEquals("""
                __kernel void jtg_kernel(__global int* input, __global int* output) {
                    int id = get_global_id(0);
                    int value = (-input[id]);
                    output[id] = ((value / 2) % 3);
                }""", Files.readString(kernelPath));
    }

    @Test
    void generatesKernelWithBitwiseIntegerOperations() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classOutputDir = Files.createTempDirectory("javatogpu-bitwise-classes");
        Path generatedOutputDir = Files.createTempDirectory("javatogpu-bitwise-generated");

        String source = """
                package sample;

                import net.sixik.ga_utils.javatogpu.api.GPU;
                import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;

                public class Demo {
                    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
                    void kernel(@GPUGlobal int[] input, @GPUGlobal int[] output) {
                        int id = GPU.get_global_id(0);
                        output[id] = ((~input[id]) << 1) ^ ((input[id] >> 1) | (input[id] & 7));
                    }
                }
                """;

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classOutputDir.toString(),
                    "-s", generatedOutputDir.toString()
            );
            JavaFileObject sourceFile = new StringJavaFileObject("sample.Demo", source);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    null,
                    options,
                    null,
                    List.of(sourceFile)
            );
            task.setProcessors(List.of(new GpuCompilerProcessor()));

            assertTrue(task.call());
        }

        Path kernelPath = generatedOutputDir.resolve("javatogpu/sample/Demo/kernel.cl");
        assertTrue(Files.exists(kernelPath));
        assertEquals("""
                __kernel void jtg_kernel(__global int* input, __global int* output) {
                    int id = get_global_id(0);
                    output[id] = (((~input[id]) << 1) ^ ((input[id] >> 1) | (input[id] & 7)));
                }""", Files.readString(kernelPath));
    }

    @Test
    void rejectsBooleanParameterDuringCompilation() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classOutputDir = Files.createTempDirectory("javatogpu-boolean-classes");
        Path generatedOutputDir = Files.createTempDirectory("javatogpu-boolean-generated");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        String source = """
                package sample;

                import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;

                public class Demo {
                    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
                    void kernel(boolean enabled, @GPUGlobal float[] output) {
                        output[0] = 1.0f;
                    }
                }
                """;

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classOutputDir.toString(),
                    "-s", generatedOutputDir.toString()
            );
            JavaFileObject sourceFile = new StringJavaFileObject("sample.Demo", source);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    options,
                    null,
                    List.of(sourceFile)
            );
            task.setProcessors(List.of(new GpuCompilerProcessor()));

            assertFalse(task.call());
        }

        assertTrue(
                diagnostics.getDiagnostics().stream().anyMatch(diagnostic ->
                        String.valueOf(diagnostic.getMessage(null)).contains("Unsupported GPU parameter type: boolean")
                )
        );
    }

    @Test
    void rejectsArrayParameterWithoutGlobalAnnotationDuringCompilation() throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        Path classOutputDir = Files.createTempDirectory("javatogpu-arrayparam-classes");
        Path generatedOutputDir = Files.createTempDirectory("javatogpu-arrayparam-generated");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        String source = """
                package sample;

                import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;

                public class Demo {
                    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
                    void kernel(float[] input, @GPUGlobal float[] output) {
                        output[0] = input[0];
                    }
                }
                """;

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            List<String> options = List.of(
                    "-classpath", System.getProperty("java.class.path"),
                    "-d", classOutputDir.toString(),
                    "-s", generatedOutputDir.toString()
            );
            JavaFileObject sourceFile = new StringJavaFileObject("sample.Demo", source);
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    options,
                    null,
                    List.of(sourceFile)
            );
            task.setProcessors(List.of(new GpuCompilerProcessor()));

            assertFalse(task.call());
        }

        assertTrue(
                diagnostics.getDiagnostics().stream().anyMatch(diagnostic ->
                        String.valueOf(diagnostic.getMessage(null)).contains(
                                "Primitive array parameters must be annotated with @GPUGlobal in the current pipeline: float[]"
                        )
                )
        );
    }

    private static final class StringJavaFileObject extends SimpleJavaFileObject {

        private final String source;

        private StringJavaFileObject(String className, String source) {
            super(URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
