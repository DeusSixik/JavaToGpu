package net.sixik.ga_utils.javatogpu.runtime.opencl;

public record OpenClPreparedArgumentBinding(
        int parameterIndex,
        OpenClPreparedBufferBinding bufferBinding,
        OpenClScalarBinding scalarBinding
) {

    public OpenClPreparedArgumentBinding {
        if ((bufferBinding == null) == (scalarBinding == null)) {
            throw new IllegalArgumentException("Exactly one prepared OpenCL argument binding kind must be set");
        }
    }

    public static OpenClPreparedArgumentBinding forBuffer(int parameterIndex, OpenClPreparedBufferBinding bufferBinding) {
        return new OpenClPreparedArgumentBinding(parameterIndex, bufferBinding, null);
    }

    public static OpenClPreparedArgumentBinding forScalar(int parameterIndex, OpenClScalarBinding scalarBinding) {
        return new OpenClPreparedArgumentBinding(parameterIndex, null, scalarBinding);
    }
}
