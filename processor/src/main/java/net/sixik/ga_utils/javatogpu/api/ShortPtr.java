package net.sixik.ga_utils.javatogpu.api;

/**
 * Mutable short-by-reference wrapper for GPU helper calls.
 */
public final class ShortPtr {

    /**
     * Wrapped scalar value.
     */
    public short value;

    public ShortPtr() {
    }

    /**
     * Creates a wrapper with an initial value.
     */
    public ShortPtr(short value) {
        this.value = value;
    }
}
