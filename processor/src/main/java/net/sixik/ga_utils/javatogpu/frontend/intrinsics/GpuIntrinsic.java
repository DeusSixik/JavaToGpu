package net.sixik.ga_utils.javatogpu.frontend.intrinsics;

import java.util.List;

public record GpuIntrinsic(
        String owner,
        String javaName,
        int arity,
        GpuIntrinsicKind kind,
        String backendName,
        String resultType,
        List<String> argumentTypes
) {
}
