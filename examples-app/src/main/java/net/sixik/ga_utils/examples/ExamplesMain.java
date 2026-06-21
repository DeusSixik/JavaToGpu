package net.sixik.ga_utils.examples;

import net.sixik.ga_utils.javatogpu.api.Float2;
import net.sixik.ga_utils.javatogpu.api.GPU;
import net.sixik.ga_utils.javatogpu.runtime.GpuRuntime;
import net.sixik.ga_utils.javatogpu.runtime.opencl.OpenClGpuRuntimeBackend;

public final class ExamplesMain {

    private ExamplesMain() {
    }

    public static void main(String[] args) {
        float[] floatInput = new float[]{1.0f, 2.0f, 3.0f, 4.0f};
        float[] basicOutput = new float[floatInput.length];
        float[] vectorOutput = new float[floatInput.length];
        float[] nativeOutput = new float[floatInput.length];
        float[] libraryOutput = new float[floatInput.length];
        float[] attributeOutput = new float[floatInput.length];

        int[] controlInput = new int[]{1, 2, 3, 4};
        int[] controlOutput = new int[controlInput.length];
        int[] atomicState = new int[]{5, 6, 7, 8};
        int[] atomicOutput = new int[atomicState.length];

        double[] doubleInput = new double[]{1.0, 2.0, 3.0, 4.0};
        double[] structOutput = new double[doubleInput.length];

        Vec2[] structBufferInput = new Vec2[]{
                new Vec2(1.0, 2.0),
                new Vec2(3.0, 4.0),
                new Vec2(5.0, 6.0),
                new Vec2(7.0, 8.0)
        };
        Vec2[] structBufferOutput = new Vec2[structBufferInput.length];
        for (int i = 0; i < structBufferOutput.length; i++) {
            structBufferOutput[i] = new Vec2();
        }

        float[] lookup = new float[]{10.0f, 20.0f, 30.0f, 40.0f};
        float[] scratch = new float[lookup.length];
        float[] localOutput = new float[lookup.length];

        try (OpenClGpuRuntimeBackend backend = new OpenClGpuRuntimeBackend()) {
            GpuRuntime.setBackend(backend);

            System.out.println("Running basic example...");
            GpuShowcase.basicMath(floatInput, basicOutput);
            System.out.println("basicOutput[0] = " + basicOutput[0]);

            System.out.println("Running control-flow example...");
            GpuShowcase.controlFlowExample(controlInput, controlOutput);
            System.out.println("controlOutput[0] = " + controlOutput[0]);

            System.out.println("Running do-while example...");
            GpuShowcase.doWhileExample(controlInput, controlOutput);
            System.out.println("doWhileOutput[0] = " + controlOutput[0]);

            System.out.println("Running vector example...");
            GpuShowcase.vectorExample(new Float2(1.0f, 0.5f), floatInput, vectorOutput);
            System.out.println("vectorOutput[0] = " + vectorOutput[0]);

            System.out.println("Running native helper example...");
            GpuShowcase.nativeHelperExample(floatInput, nativeOutput);
            System.out.println("nativeOutput[0] = " + nativeOutput[0]);

            System.out.println("Running @CCodeLibrary example...");
            GpuShowcase.libraryHelperExample(floatInput, libraryOutput);
            System.out.println("libraryOutput[0] = " + libraryOutput[0]);

            System.out.println("Running @OpenCLAttributes example...");
            GpuShowcase.attributeExample(floatInput, attributeOutput);
            System.out.println("attributeOutput[0] = " + attributeOutput[0]);

            System.out.println("Running struct example...");
            GpuShowcase.structExample(
                    new SampleData(0.75, 3),
                    doubleInput,
                    structOutput
            );
            System.out.println("structOutput[0] = " + structOutput[0]);

            System.out.println("Running struct buffer example...");
            GpuShowcase.structBufferExample(structBufferInput, structBufferOutput);
            System.out.println("structBufferOutput[0] = (" + structBufferOutput[0].x + ", " + structBufferOutput[0].y + ")");

            System.out.println("Running atomic example...");
            GpuShowcase.atomicExample(atomicState, atomicOutput);
            System.out.println("atomicOutput[0] = " + atomicOutput[0]);

            System.out.println("Running local memory example...");
            GpuShowcase.localMemoryExample(lookup, scratch, localOutput);
            System.out.println("localOutput[0] = " + localOutput[0]);
        } catch (RuntimeException exception) {
            System.out.println("GPU execution failed: " + exception.getMessage());
        } finally {
            GpuRuntime.setBackend(GpuRuntime.defaultBackend());
        }

        System.out.println("Running ASM compiler example...");
        System.out.println(AsmExamples.compileStructuredAsmExample());
    }
}
