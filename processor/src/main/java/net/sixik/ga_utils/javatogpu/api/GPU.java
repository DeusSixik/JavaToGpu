package net.sixik.ga_utils.javatogpu.api;

import net.sixik.ga_utils.javatogpu.api.anotations.GPUIntrinsic;

/**
 * Java facade for GPU built-ins available inside {@code @GPU} kernels.
 *
 * <p>The methods in this class intentionally look like ordinary Java methods so user code remains valid Java, but the
 * annotation processor maps them directly to backend intrinsics such as {@code get_global_id}, {@code sin} or
 * {@code barrier}.
 *
 * <p>Example:
 *
 * <pre>{@code
 * @net.sixik.ga_utils.javatogpu.api.anotations.GPU
 * static void kernel(
 *         @net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal float[] input,
 *         @net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal float[] output
 * ) {
 *     int id = GPU.get_global_id(0);
 *     output[id] = GPU.sin(input[id]) + GPU.cos(input[id]);
 * }
 * }</pre>
 *
 * <p>Outside translated GPU code these methods behave like light Java stubs or JVM fallbacks. Their real purpose is to
 * provide a stable source-level API for code generation.
 */
public final class GPU {

    /**
     * OpenCL flag for synchronizing accesses to {@code __local} memory.
     */
    public static final int CLK_LOCAL_MEM_FENCE = 1;

    /**
     * OpenCL flag for synchronizing accesses to {@code __global} memory.
     */
    public static final int CLK_GLOBAL_MEM_FENCE = 2;

    private static final double LOG_2 = Math.log(2.0);

    private GPU() {
    }

    @GPUIntrinsic(name = "get_work_dim")
    public static int get_work_dim() {
        return 1;
    }

    @GPUIntrinsic(name = "get_global_size")
    public static int get_global_size(int dimension) {
        return 1;
    }

    @GPUIntrinsic(name = "sin")
    public static float sin(float value) {
        return (float) Math.sin(value);
    }

    @GPUIntrinsic(name = "sin")
    public static double sin(double value) {
        return Math.sin(value);
    }

    @GPUIntrinsic(name = "cos")
    public static float cos(float value) {
        return (float) Math.cos(value);
    }

    @GPUIntrinsic(name = "cos")
    public static double cos(double value) {
        return Math.cos(value);
    }

    @GPUIntrinsic(name = "tan")
    public static float tan(float value) {
        return (float) Math.tan(value);
    }

    @GPUIntrinsic(name = "tan")
    public static double tan(double value) {
        return Math.tan(value);
    }

    @GPUIntrinsic(name = "sqrt")
    public static float sqrt(float value) {
        return (float) Math.sqrt(value);
    }

    @GPUIntrinsic(name = "sqrt")
    public static double sqrt(double value) {
        return Math.sqrt(value);
    }

    @GPUIntrinsic(name = "exp")
    public static float exp(float value) {
        return (float) Math.exp(value);
    }

    @GPUIntrinsic(name = "exp")
    public static double exp(double value) {
        return Math.exp(value);
    }

    @GPUIntrinsic(name = "log")
    public static float log(float value) {
        return (float) Math.log(value);
    }

    @GPUIntrinsic(name = "log")
    public static double log(double value) {
        return Math.log(value);
    }

    @GPUIntrinsic(name = "log2")
    public static float log2(float value) {
        return (float) (Math.log(value) / LOG_2);
    }

    @GPUIntrinsic(name = "log2")
    public static double log2(double value) {
        return Math.log(value) / LOG_2;
    }

    @GPUIntrinsic(name = "fabs")
    public static float fabs(float value) {
        return Math.abs(value);
    }

    @GPUIntrinsic(name = "fabs")
    public static double fabs(double value) {
        return Math.abs(value);
    }

    @GPUIntrinsic(name = "fabs")
    public static float abs(float value) {
        return Math.abs(value);
    }

    @GPUIntrinsic(name = "fabs")
    public static double abs(double value) {
        return Math.abs(value);
    }

    @GPUIntrinsic(name = "floor")
    public static float floor(float value) {
        return (float) Math.floor(value);
    }

    @GPUIntrinsic(name = "floor")
    public static double floor(double value) {
        return Math.floor(value);
    }

    @GPUIntrinsic(name = "ceil")
    public static float ceil(float value) {
        return (float) Math.ceil(value);
    }

    @GPUIntrinsic(name = "ceil")
    public static double ceil(double value) {
        return Math.ceil(value);
    }

    @GPUIntrinsic(name = "pow")
    public static float pow(float left, float right) {
        return (float) Math.pow(left, right);
    }

    @GPUIntrinsic(name = "pow")
    public static double pow(double left, double right) {
        return Math.pow(left, right);
    }

    @GPUIntrinsic(name = "min")
    public static float min(float left, float right) {
        return Math.min(left, right);
    }

    @GPUIntrinsic(name = "min")
    public static double min(double left, double right) {
        return Math.min(left, right);
    }

    @GPUIntrinsic(name = "max")
    public static float max(float left, float right) {
        return Math.max(left, right);
    }

    @GPUIntrinsic(name = "max")
    public static double max(double left, double right) {
        return Math.max(left, right);
    }

    @GPUIntrinsic(name = "rsqrt")
    public static float rsqrt(float value) {
        return 1.0f / (float) Math.sqrt(value);
    }

    @GPUIntrinsic(name = "rsqrt")
    public static double rsqrt(double value) {
        return 1.0 / Math.sqrt(value);
    }

    @GPUIntrinsic(name = "fmin")
    public static float fmin(float left, float right) {
        return Math.min(left, right);
    }

    @GPUIntrinsic(name = "fmin")
    public static double fmin(double left, double right) {
        return Math.min(left, right);
    }

    @GPUIntrinsic(name = "fmax")
    public static float fmax(float left, float right) {
        return Math.max(left, right);
    }

    @GPUIntrinsic(name = "fmax")
    public static double fmax(double left, double right) {
        return Math.max(left, right);
    }

    @GPUIntrinsic(name = "mad")
    public static float mad(float a, float b, float c) {
        return a * b + c;
    }

    @GPUIntrinsic(name = "mad")
    public static double mad(double a, double b, double c) {
        return a * b + c;
    }

    @GPUIntrinsic(name = "clamp")
    public static float clamp(float value, float minValue, float maxValue) {
        return Math.max(minValue, Math.min(value, maxValue));
    }

    @GPUIntrinsic(name = "clamp")
    public static double clamp(double value, double minValue, double maxValue) {
        return Math.max(minValue, Math.min(value, maxValue));
    }

    @GPUIntrinsic(name = "mix")
    public static float mix(float left, float right, float amount) {
        return left + (right - left) * amount;
    }

    @GPUIntrinsic(name = "mix")
    public static double mix(double left, double right, double amount) {
        return left + (right - left) * amount;
    }

    @GPUIntrinsic(name = "step")
    public static float step(float edge, float value) {
        return value < edge ? 0.0f : 1.0f;
    }

    @GPUIntrinsic(name = "step")
    public static double step(double edge, double value) {
        return value < edge ? 0.0 : 1.0;
    }

    @GPUIntrinsic(name = "smoothstep")
    public static float smoothstep(float edge0, float edge1, float value) {
        float t = clamp((value - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    @GPUIntrinsic(name = "smoothstep")
    public static double smoothstep(double edge0, double edge1, double value) {
        double t = clamp((value - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    @GPUIntrinsic(name = "hypot")
    public static float length(float x, float y) {
        return (float) Math.hypot(x, y);
    }

    @GPUIntrinsic(name = "hypot")
    public static double length(double x, double y) {
        return Math.hypot(x, y);
    }

    @GPUIntrinsic(code = "(({0}) - floor({0}))")
    public static float fract(float value) {
        return value - (float) Math.floor(value);
    }

    @GPUIntrinsic(code = "(({0}) - floor({0}))")
    public static double fract(double value) {
        return value - Math.floor(value);
    }

    @GPUIntrinsic(name = "get_global_id")
    public static int get_global_id(int dimension) {
        return 0;
    }

    @GPUIntrinsic(name = "get_local_size")
    public static int get_local_size(int dimension) {
        return 1;
    }

    @GPUIntrinsic(name = "get_local_id")
    public static int get_local_id(int dimension) {
        return 0;
    }

    @GPUIntrinsic(name = "get_num_groups")
    public static int get_num_groups(int dimension) {
        return 1;
    }

    @GPUIntrinsic(name = "get_group_id")
    public static int get_group_id(int dimension) {
        return 0;
    }

    @GPUIntrinsic(name = "barrier")
    public static void barrier(int flags) {
    }
}
