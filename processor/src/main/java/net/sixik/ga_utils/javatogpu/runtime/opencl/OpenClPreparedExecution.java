package net.sixik.ga_utils.javatogpu.runtime.opencl;

import java.util.List;

public record OpenClPreparedExecution(
        OpenClCompiledKernel compiledKernel,
        java.util.List<OpenClPreparedBufferBinding> bufferBindings,
        java.util.List<OpenClLocalBinding> localBindings,
        java.util.List<OpenClScalarBinding> scalarBindings,
        java.util.List<OpenClPreparedArgumentBinding> argumentBindings
) {
}
