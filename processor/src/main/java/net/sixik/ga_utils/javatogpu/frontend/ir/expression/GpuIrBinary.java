package net.sixik.ga_utils.javatogpu.frontend.ir.expression;

public record GpuIrBinary(
        String operator,
        GpuIrExpression left,
        GpuIrExpression right
) implements GpuIrExpression {
}
