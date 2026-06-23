package net.sixik.ga_utils.javatogpu.api;

import net.sixik.ga_utils.javatogpu.api.anotations.GPUVectorType;

/**
 * Java-side representation of the OpenCL {@code double2} vector type.
 */
@GPUVectorType(openClType = "double2", componentType = "double", fields = {"x", "y"})
public class Double2 {

    /**
     * First vector component.
     */
    public double x;
    /**
     * Second vector component.
     */
    public double y;

    public Double2() {
    }

    /**
     * Broadcast constructor. Fills all components with the same value.
     */
    public Double2(double value) {
        this.x = value;
        this.y = value;
    }

    /**
     * Creates a vector from explicit components.
     */
    public Double2(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
