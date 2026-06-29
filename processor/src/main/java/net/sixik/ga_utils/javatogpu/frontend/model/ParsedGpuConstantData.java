package net.sixik.ga_utils.javatogpu.frontend.model;

public record ParsedGpuConstantData(
        String ownerSimpleName,
        String ownerQualifiedName,
        String name,
        String javaType,
        String initializerSource,
        GpuConstantDataKind kind
) {
}
