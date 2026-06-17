package net.sixik.ga_utils.javatogpu.frontend.ir.statement;

import net.sixik.ga_utils.javatogpu.frontend.ir.expression.GpuIrExpression;

import java.util.List;

public record GpuIrIf(
        GpuIrExpression condition,
        List<GpuIrStatement> thenBranch,
        List<GpuIrStatement> elseBranch
) implements GpuIrStatement {
}
