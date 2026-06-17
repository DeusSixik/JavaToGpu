package net.sixik.ga_utils.javatogpu.runtime.opencl;

public record OpenClPlannedArgumentBinding(
        int parameterIndex,
        OpenClBufferBinding bufferBinding,
        OpenClScalarBinding scalarBinding
) {

    public OpenClPlannedArgumentBinding {
        if ((bufferBinding == null) == (scalarBinding == null)) {
            throw new IllegalArgumentException("Exactly one planned OpenCL argument binding kind must be set");
        }
    }

    public static OpenClPlannedArgumentBinding forBuffer(int parameterIndex, OpenClBufferBinding bufferBinding) {
        return new OpenClPlannedArgumentBinding(parameterIndex, bufferBinding, null);
    }

    public static OpenClPlannedArgumentBinding forScalar(int parameterIndex, OpenClScalarBinding scalarBinding) {
        return new OpenClPlannedArgumentBinding(parameterIndex, null, scalarBinding);
    }
}
