package net.sixik.ga_utils.javatogpu.frontend.ir.expression;

public sealed interface GpuIrExpression permits GpuIrArrayAccess, GpuIrBinary, GpuIrCast, GpuIrFieldAccess, GpuIrHelperCall, GpuIrIntrinsicCall, GpuIrLiteral, GpuIrStructInit, GpuIrTernary, GpuIrUnary, GpuIrVariableRef {
}
