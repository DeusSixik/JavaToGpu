package net.sixik.ga_utils.javatogpu.api;

/**
 * Java-side representation of the OpenCL {@code long4} vector type.
 */
public class Long4 {

    /**
     * First vector component.
     */
    public long x;
    /**
     * Second vector component.
     */
    public long y;
    /**
     * Third vector component.
     */
    public long z;
    /**
     * Fourth vector component.
     */
    public long w;

    public Long4() {
    }

    /**
     * Broadcast constructor. Fills all components with the same value.
     */
    public Long4(long value) {
        this.x = value;
        this.y = value;
        this.z = value;
        this.w = value;
    }

    /**
     * Creates a vector from explicit components.
     */
    public Long4(long x, long y, long z, long w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
}
