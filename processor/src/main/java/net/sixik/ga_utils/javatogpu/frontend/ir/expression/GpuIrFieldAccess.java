package net.sixik.ga_utils.javatogpu.frontend.ir.expression;

public record GpuIrFieldAccess(
        GpuIrExpression target,
        String fieldName
) implements GpuIrExpression {
}
