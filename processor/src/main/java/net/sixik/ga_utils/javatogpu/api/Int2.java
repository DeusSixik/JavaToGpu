package net.sixik.ga_utils.javatogpu.api;

/**
 * Java-side representation of the OpenCL {@code int2} vector type.
 */
public class Int2 {

    /**
     * First vector component.
     */
    public int x;
    /**
     * Second vector component.
     */
    public int y;

    public Int2() {
    }

    /**
     * Broadcast constructor. Fills all components with the same value.
     */
    public Int2(int value) {
        this.x = value;
        this.y = value;
    }

    /**
     * Creates a vector from explicit components.
     */
    public Int2(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
