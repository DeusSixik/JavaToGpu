package net.sixik.ga_utils.javatogpu.api;

/**
 * Mutable char-by-reference wrapper for GPU helper calls.
 */
public final class CharPtr {

    /**
     * Wrapped scalar value.
     */
    public char value;

    public CharPtr() {
    }

    /**
     * Creates a wrapper with an initial value.
     */
    public CharPtr(char value) {
        this.value = value;
    }
}
