package net.sixik.ga_utils.javatogpu.api;

/**
 * Java-side representation of the OpenCL {@code float4} vector type.
 */
public class Float4 {

    /**
     * First vector component.
     */
    public float x;
    /**
     * Second vector component.
     */
    public float y;
    /**
     * Third vector component.
     */
    public float z;
    /**
     * Fourth vector component.
     */
    public float w;

    public Float4() {
    }

    /**
     * Broadcast constructor. Fills all components with the same value.
     */
    public Float4(float value) {
        this.x = value;
        this.y = value;
        this.z = value;
        this.w = value;
    }

    /**
     * Creates a vector from explicit components.
     */
    public Float4(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
}
