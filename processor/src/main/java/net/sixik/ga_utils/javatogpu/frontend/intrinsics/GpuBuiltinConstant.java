package net.sixik.ga_utils.javatogpu.frontend.intrinsics;

public record GpuBuiltinConstant(
        String ownerSimpleName,
        String ownerQualifiedName,
        String name,
        String javaType,
        String sourceText
) {
}
