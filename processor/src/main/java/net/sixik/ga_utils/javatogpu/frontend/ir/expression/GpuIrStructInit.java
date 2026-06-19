package net.sixik.ga_utils.javatogpu.frontend.ir.expression;

import java.util.List;

public record GpuIrStructInit(
        String structType,
        List<GpuIrExpression> arguments
) implements GpuIrExpression {
}
