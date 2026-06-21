package net.sixik.ga_utils.examples;

import net.sixik.ga_utils.javatogpu.api.anotations.GPUStruct;

@GPUStruct
public final class SampleData {

    public double bias;
    public int index;

    public SampleData() {
    }

    public SampleData(double bias, int index) {
        this.bias = bias;
        this.index = index;
    }
}
