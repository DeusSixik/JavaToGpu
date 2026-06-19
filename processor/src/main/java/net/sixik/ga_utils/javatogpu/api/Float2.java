package net.sixik.ga_utils.javatogpu.api;

/**
 * Java-side representation of the OpenCL {@code float2} vector type.
 *
 * <p>Use this type in local variables, helper parameters/returns and struct fields.
 *
 * <pre>{@code
 * Float2 uv = new Float2(0.5f, 1.0f);
 * float sum = uv.x + uv.y;
 * }</pre>
 */
public class Float2 {

    /**
     * First vector component.
     */
    public float x;

    /**
     * Second vector component.
     */
    public float y;

    public Float2() {
    }

    /**
     * Broadcast constructor. Fills all components with the same value.
     */
    public Float2(float value) {
        this.x = value;
        this.y = value;
    }

    /**
     * Creates a vector from explicit components.
     */
    public Float2(float x, float y) {
        this.x = x;
        this.y = y;
    }
}
