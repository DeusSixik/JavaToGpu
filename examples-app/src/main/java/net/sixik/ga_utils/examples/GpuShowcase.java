package net.sixik.ga_utils.examples;

import net.sixik.ga_utils.javatogpu.api.Float2;
import net.sixik.ga_utils.javatogpu.api.FloatPtr;
import net.sixik.ga_utils.javatogpu.api.GPU;
import net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal;
import net.sixik.ga_utils.javatogpu.api.anotations.GPULocal;
import net.sixik.ga_utils.javatogpu.api.anotations.GPUConstant;
import net.sixik.ga_utils.javatogpu.api.anotations.OpenCLAttributes;

public final class GpuShowcase {

    private GpuShowcase() {
    }

    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
    public static void basicMath(
            @GPUGlobal float[] input,
            @GPUGlobal float[] output
    ) {
        int id = GPU.get_global_id(0);
        FloatPtr ptr = new FloatPtr(input[id]);

        GpuSupport.clamp(ptr);
        output[id] = GpuSupport.lerp(ptr.value, GPU.sin(input[id]), 0.25f);
    }

    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
    public static void controlFlowExample(
            @GPUGlobal int[] input,
            @GPUGlobal int[] output
    ) {
        int id = GPU.get_global_id(0);
        int value = input[id];
        int step = 0;

        while (step < 3) {
            switch (step) {
                case 0:
                    value += 2;
                    break;
                case 1:
                    value *= 2;
                    break;
                default:
                    value -= 1;
                    break;
            }
            step++;
        }

        output[id] = value;
    }

    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
    public static void doWhileExample(
            @GPUGlobal int[] input,
            @GPUGlobal int[] output
    ) {
        int id = GPU.get_global_id(0);
        int value = input[id];
        int step = 0;

        do {
            value = GPU.max(value, step);
            step++;
        } while (step < 2);

        output[id] = value;
    }

    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
    public static void vectorExample(
            Float2 bias,
            @GPUGlobal float[] input,
            @GPUGlobal float[] output
    ) {
        int id = GPU.get_global_id(0);

        Float2 left = new Float2(input[id], input[id] * 2.0f);
        Float2 sum = GpuSupport.add(left, bias);

        output[id] = sum.x + sum.y;
    }

    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
    public static void nativeHelperExample(
            @GPUGlobal float[] input,
            @GPUGlobal float[] output
    ) {
        int id = GPU.get_global_id(0);
        output[id] = GpuSupport.rawBlend(input[id], 0.1f);
    }

    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
    public static void libraryHelperExample(
            @GPUGlobal float[] input,
            @GPUGlobal float[] output
    ) {
        int id = GPU.get_global_id(0);
        output[id] = ReusableMathLibrary.norm(input[id]);
    }

    @OpenCLAttributes({"work_group_size_hint(4, 1, 1)"})
    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
    public static void attributeExample(
            @GPUGlobal float[] input,
            @GPUGlobal float[] output
    ) {
        int id = GPU.get_global_id(0);
        output[id] = GPU.max(input[id], 1.0f) + 1.0f;
    }

    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
    public static void structExample(
            SampleData sample,
            @GPUGlobal double[] input,
            @GPUGlobal double[] output
    ) {
        int id = GPU.get_global_id(0);

        Vec2 point = new Vec2(input[id], input[id] * 2.0);
        SampleData localSample = new SampleData(0.5, id);

        output[id] = point.x + point.y + sample.bias + sample.index
                + localSample.bias + localSample.index;
    }

    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
    public static void structBufferExample(
            @GPUGlobal Vec2[] input,
            @GPUGlobal Vec2[] output
    ) {
        int id = GPU.get_global_id(0);
        output[id].x = input[id].x + 1.0;
        output[id].y = input[id].y + 2.0;
    }

    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
    public static void atomicExample(
            @GPUGlobal int[] state,
            @GPUGlobal int[] output
    ) {
        int id = GPU.get_global_id(0);
        int previous = GPU.atomic_add(state, id, 2);
        output[id] = previous + GPU.atomic_xor(state, id, 31);
    }

    @net.sixik.ga_utils.javatogpu.api.anotations.GPU
    public static void localMemoryExample(
            @GPUConstant float[] lookup,
            @GPULocal float[] scratch,
            @GPUGlobal float[] output
    ) {
        int gid = GPU.get_global_id(0);
        int lid = GPU.get_local_id(0);

        scratch[lid] = lookup[lid];
        GPU.local_mem_fence();
        GPU.local_barrier();
        output[gid] = scratch[lid];
    }
}
