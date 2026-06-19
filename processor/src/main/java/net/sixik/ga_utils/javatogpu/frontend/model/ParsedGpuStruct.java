package net.sixik.ga_utils.javatogpu.frontend.model;

import java.util.List;

public record ParsedGpuStruct(
        String ownerSimpleName,
        String ownerQualifiedName,
        List<ParsedGpuStructField> fields,
        List<ParsedGpuConstant> constants,
        List<String> openClAttributes
) {
}
