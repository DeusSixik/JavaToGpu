package net.sixik.ga_utils.javatogpu.runtime.opencl;

public record OpenClPreparedArgumentBinding(
        int parameterIndex,
        OpenClPreparedBufferBinding bufferBinding,
        OpenClLocalBinding localBinding,
        OpenClScalarBinding scalarBinding
) {

    public OpenClPreparedArgumentBinding {
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
            throw new IllegalArgumentException("Exactly one prepared OpenCL argument binding kind must be set");
        }
    }

    public static OpenClPreparedArgumentBinding forBuffer(int parameterIndex, OpenClPreparedBufferBinding bufferBinding) {
        return new OpenClPreparedArgumentBinding(parameterIndex, bufferBinding, null, null);
    }

    public static OpenClPreparedArgumentBinding forLocal(int parameterIndex, OpenClLocalBinding localBinding) {
        return new OpenClPreparedArgumentBinding(parameterIndex, null, localBinding, null);
    }

    public static OpenClPreparedArgumentBinding forScalar(int parameterIndex, OpenClScalarBinding scalarBinding) {
        return new OpenClPreparedArgumentBinding(parameterIndex, null, null, scalarBinding);
    }
}
