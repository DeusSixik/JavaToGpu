package net.sixik.ga_utils.javatogpu.runtime.opencl;

import java.util.List;

public record OpenClExecutionPlan(
        java.util.List<OpenClBufferBinding> bufferBindings,
        java.util.List<OpenClScalarBinding> scalarBindings,
        java.util.List<OpenClPlannedArgumentBinding> argumentBindings
) {
}
