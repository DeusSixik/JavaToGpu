package net.sixik.ga_utils.javatogpu.frontend.ir.expression;

public record GpuIrTernary(
        GpuIrExpression condition,
        GpuIrExpression whenTrue,
        GpuIrExpression whenFalse
) implements GpuIrExpression {
}
