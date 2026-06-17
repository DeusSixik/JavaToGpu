package net.sixik.ga_utils.javatogpu.frontend.ir.expression;

public record GpuIrArrayAccess(String arrayName, GpuIrExpression index) implements GpuIrExpression {
}
