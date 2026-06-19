package net.sixik.ga_utils.javatogpu.api;

/**
 * Mutable int-by-reference wrapper for GPU helper calls.
 */
public final class IntPtr {

    /**
     * Wrapped scalar value.
     */
    public int value;

    public IntPtr() {
    }

    /**
     * Creates a wrapper with an initial value.
     */
    public IntPtr(int value) {
        this.value = value;
    }
}
