package net.sixik.ga_utils.javatogpu.frontend.model;

import java.util.List;

public record ParsedGpuStructField(
        String name,
        String javaType,
        List<String> openClAttributes
) {
}
