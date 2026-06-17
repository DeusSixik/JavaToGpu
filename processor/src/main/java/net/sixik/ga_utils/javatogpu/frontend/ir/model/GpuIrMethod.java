package net.sixik.ga_utils.javatogpu.frontend.ir.model;

import net.sixik.ga_utils.javatogpu.frontend.ir.statement.GpuIrStatement;

import java.util.List;

public record GpuIrMethod(
        String name,
        List<GpuIrStatement> statements
) {
}
