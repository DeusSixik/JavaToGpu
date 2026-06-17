package net.sixik.ga_utils.javatogpu.api;

public final class GPU {

    private GPU() {
    }

    public static float sin(float value) {
        throw unsupported();
    }

    public static float cos(float value) {
        throw unsupported();
    }

    public static float sqrt(float value) {
        throw unsupported();
    }

    public static float fabs(float value) {
        throw unsupported();
    }

    public static int get_global_id(int dimension) {
        throw unsupported();
    }

    private static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("GPU intrinsics are compile-time only");
    }
}
