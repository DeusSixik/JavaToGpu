package net.sixik.ga_utils.javatogpu.api;

/**
 * Java-side representation of the OpenCL {@code float3} vector type.
 */
public class Float3 {

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

    public Float3() {
    }

    /**
     * Broadcast constructor. Fills all components with the same value.
     */
    public Float3(float value) {
        this.x = value;
        this.y = value;
        this.z = value;
    }

    /**
     * Creates a vector from explicit components.
     */
    public Float3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}
