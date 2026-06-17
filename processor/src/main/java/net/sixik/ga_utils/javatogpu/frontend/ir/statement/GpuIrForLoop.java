package net.sixik.ga_utils.javatogpu.frontend.ir.statement;

import net.sixik.ga_utils.javatogpu.frontend.ir.expression.GpuIrExpression;

import java.util.List;

public record GpuIrForLoop(
        GpuIrStatement initializer,
        GpuIrExpression condition,
        GpuIrStatement update,
        List<GpuIrStatement> body
) implements GpuIrStatement {
}
