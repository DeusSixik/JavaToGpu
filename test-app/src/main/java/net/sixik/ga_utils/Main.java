package net.sixik.ga_utils;

import net.sixik.ga_utils.javatogpu.api.Float2;
import net.sixik.ga_utils.javatogpu.api.FloatPtr;
import net.sixik.ga_utils.javatogpu.api.GPU;
import net.sixik.ga_utils.javatogpu.api.anotations.CCode;
import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;
import net.sixik.ga_utils.javatogpu.api.anotations.GPUStruct;
import net.sixik.ga_utils.javatogpu.runtime.GpuRuntime;
import net.sixik.ga_utils.javatogpu.runtime.opencl.OpenClGpuRuntimeBackend;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        float[] floatInput = new float[]{1.0f, 2.0f, 3.0f, 4.0f};
        float[] basicOutput = new float[floatInput.length];
        float[] vectorOutput = new float[floatInput.length];

        double[] doubleInput = new double[]{1.0, 2.0, 3.0, 4.0};
        double[] structOutput = new double[doubleInput.length];

        try (OpenClGpuRuntimeBackend backend = new OpenClGpuRuntimeBackend()) {
            GpuRuntime.setBackend(backend);

            System.out.println("Running basic @GPU example...");
            Examples.basicMath(floatInput, basicOutput);
            System.out.println("basicOutput[0] = " + basicOutput[0]);

            System.out.println("Running @GPUStruct example...");
            Examples.structExample(doubleInput, structOutput);
            System.out.println("structOutput[0] = " + structOutput[0]);

            System.out.println("Running vector example...");
            Examples.vectorExample(floatInput, vectorOutput);
            System.out.println("vectorOutput[0] = " + vectorOutput[0]);
        } catch (RuntimeException exception) {
            System.out.println("GPU execution failed: " + exception.getMessage());
        } finally {
            GpuRuntime.setBackend(GpuRuntime.defaultBackend());
        }
    }

    public static final class Examples {

        private Examples() {
        }

        @net.sixik.ga_utils.javatogpu.api.anotations.GPU
        public static void basicMath(
                @GPUGlobal float[] input,
                @GPUGlobal float[] output
        ) {
            int id = GPU.get_global_id(0);
            FloatPtr ptr = new FloatPtr(input[id]);

            KernelMath.clamp(ptr);
            output[id] = KernelMath.lerp(ptr.value, GPU.sin(input[id]), 0.25f);
        }

        @net.sixik.ga_utils.javatogpu.api.anotations.GPU
        public static void structExample(
                @GPUGlobal double[] input,
                @GPUGlobal double[] output
        ) {
            int id = GPU.get_global_id(0);

            SamplePoint point = new SamplePoint(input[id], input[id] * 2.0);
            SampleData sample = new SampleData(point, 0.5, id);

            output[id] = sample.point.x + sample.point.y + sample.bias + sample.index;
        }

        @net.sixik.ga_utils.javatogpu.api.anotations.GPU
        public static void vectorExample(
                @GPUGlobal float[] input,
                @GPUGlobal float[] output
        ) {
            int id = GPU.get_global_id(0);

            Float2 left = new Float2(input[id], input[id] * 2.0f);
            Float2 right = new Float2(1.0f);
            Float2 sum = VectorMath.add(left, right);

            output[id] = sum.x + sum.y;
        }
    }

    public static final class KernelMath {

        private static final float LIMIT = 32.0f;

        private KernelMath() {
        }

        @CCode
        public static void clamp(FloatPtr ptr) {
            if (ptr.value > LIMIT) {
                ptr.value = LIMIT;
            }
        }

        @CCode(inline = true)
        public static float lerp(float a, float b, float t) {
            return a + (b - a) * t;
        }
    }

    public static final class VectorMath {

        private VectorMath() {
        }

        @CCode
        public static Float2 add(Float2 left, Float2 right) {
            return new Float2(left.x + right.x, left.y + right.y);
        }
    }

    @GPUStruct
    public static final class SamplePoint {

        public double x;
        public double y;

        public SamplePoint() {
        }

        public SamplePoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    @GPUStruct
    public static final class SampleData {

        public SamplePoint point;
        public double bias;
        public int index;

        public SampleData() {
        }

        public SampleData(SamplePoint point, double bias, int index) {
            this.point = point;
            this.bias = bias;
            this.index = index;
        }
    }
}
