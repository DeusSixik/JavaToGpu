package net.sixik.ga_utils.javatogpu.api;

/**
 * Java-side representation of the OpenCL {@code long3} vector type.
 */
public class Long3 {

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

    public Long3() {
    }

    /**
     * Broadcast constructor. Fills all components with the same value.
     */
    public Long3(long value) {
        this.x = value;
        this.y = value;
        this.z = value;
    }

    /**
     * Creates a vector from explicit components.
     */
    public Long3(long x, long y, long z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
