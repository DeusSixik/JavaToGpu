package net.sixik.ga_utils.javatogpu.api;

/**
 * Java-side representation of the OpenCL {@code long2} vector type.
 */
public class Long2 {

    /**
     * First vector component.
     */
    public long x;
    /**
     * Second vector component.
     */
    public long y;

    public Long2() {
    }

    /**
     * Broadcast constructor. Fills all components with the same value.
     */
    public Long2(long value) {
        this.x = value;
        this.y = value;
    }

    /**
     * Creates a vector from explicit components.
     */
    public Long2(long x, long y) {
        this.x = x;
        this.y = y;
    }
}
