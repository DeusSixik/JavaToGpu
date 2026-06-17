package net.sixik.ga_utils.javatogpu.frontend.ir.expression;

public record GpuIrUnary(
        String operator,
        GpuIrExpression operand
) implements GpuIrExpression {
}
