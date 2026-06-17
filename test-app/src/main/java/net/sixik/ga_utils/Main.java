package net.sixik.ga_utils;

import net.sixik.ga_utils.javatogpu.api.anotations.GPU;
import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;
import net.sixik.ga_utils.javatogpu.runtime.GpuRuntime;
import net.sixik.ga_utils.javatogpu.runtime.opencl.OpenClGpuRuntimeBackend;

public class Main {
    public static void main(String[] args) {
        float[] in = new float[256];
        float[] out = new float[256];

        try (OpenClGpuRuntimeBackend backend = new OpenClGpuRuntimeBackend()) {
            GpuRuntime.setBackend(backend);
            System.out.println("Invoking @GPU method directly...");
            GpuTest.my_gpu_code(in, out);
            System.out.println("GPU result: " + out[0]);
        } catch (RuntimeException exception) {
            System.out.println("GPU execution failed: " + exception.getMessage());
        } finally {
            GpuRuntime.setBackend(GpuRuntime.defaultBackend());
        }
    }

    public static class GpuTest {

        @GPU
        public static void my_gpu_code(
                @GPUGlobal float[] input,
                @GPUGlobal float[] output
        ) {
            output[0] = 5 + 4;
        }
    }
}
