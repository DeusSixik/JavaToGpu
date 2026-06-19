package net.sixik.ga_utils.javatogpu.api;

/**
 * Mutable long-by-reference wrapper for GPU helper calls.
 */
public final class LongPtr {

    /**
     * Wrapped scalar value.
     */
    public long value;

    public LongPtr() {
    }

    /**
     * Creates a wrapper with an initial value.
     */
    public LongPtr(long value) {
        this.value = value;
    }
}
