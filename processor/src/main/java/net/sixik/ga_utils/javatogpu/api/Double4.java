package net.sixik.ga_utils.javatogpu.api;

/**
 * Java-side representation of the OpenCL {@code double4} vector type.
 */
public class Double4 {

    /**
     * First vector component.
     */
    public double x;
    /**
     * Second vector component.
     */
    public double y;
    /**
     * Third vector component.
     */
    public double z;
    /**
     * Fourth vector component.
     */
    public double w;

    public Double4() {
    }

    /**
     * Broadcast constructor. Fills all components with the same value.
     */
    public Double4(double value) {
        this.x = value;
        this.y = value;
        this.z = value;
        this.w = value;
    }

    /**
     * Creates a vector from explicit components.
     */
    public Double4(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
}
