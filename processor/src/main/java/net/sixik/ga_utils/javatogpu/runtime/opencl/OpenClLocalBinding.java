package net.sixik.ga_utils.javatogpu.runtime.opencl;

public record OpenClLocalBinding(
        OpenClArgumentKind kind,
        Object sourceArray,
        int length,
        long byteSize
) {
}
