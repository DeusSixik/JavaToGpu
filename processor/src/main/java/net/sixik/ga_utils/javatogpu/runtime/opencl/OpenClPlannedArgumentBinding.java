package net.sixik.ga_utils.javatogpu.runtime.opencl;

public record OpenClPlannedArgumentBinding(
        int parameterIndex,
        OpenClBufferBinding bufferBinding,
        OpenClLocalBinding localBinding,
        OpenClScalarBinding scalarBinding
) {

    public OpenClPlannedArgumentBinding {
        int kinds = 0;
        if (bufferBinding != null) {
            kinds++;
        }
        if (localBinding != null) {
            kinds++;
        }
        if (scalarBinding != null) {
            kinds++;
        }
        if (kinds != 1) {
            throw new IllegalArgumentException("Exactly one planned OpenCL argument binding kind must be set");
        }
    }

    public static OpenClPlannedArgumentBinding forBuffer(int parameterIndex, OpenClBufferBinding bufferBinding) {
        return new OpenClPlannedArgumentBinding(parameterIndex, bufferBinding, null, null);
    }

    public static OpenClPlannedArgumentBinding forLocal(int parameterIndex, OpenClLocalBinding localBinding) {
        return new OpenClPlannedArgumentBinding(parameterIndex, null, localBinding, null);
    }

    public static OpenClPlannedArgumentBinding forScalar(int parameterIndex, OpenClScalarBinding scalarBinding) {
        return new OpenClPlannedArgumentBinding(parameterIndex, null, null, scalarBinding);
    }
}
