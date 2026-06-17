package net.sixik.ga_utils.javatogpu.runtime.opencl;

import java.util.List;

public final class OpenClExecutionPreparer {

    private final OpenClDeviceBufferRegistry registry;

    public OpenClExecutionPreparer(OpenClDeviceBufferRegistry registry) {
        this.registry = registry;
    }

    public OpenClPreparedExecution prepare(OpenClCompiledKernel compiledKernel, OpenClExecutionPlan plan) {
        List<OpenClPreparedBufferBinding> preparedBuffers = plan.bufferBindings().stream()
                .map(binding -> new OpenClPreparedBufferBinding(binding, registry.acquire(binding)))
                .toList();
        java.util.Map<String, OpenClPreparedBufferBinding> preparedBufferByHandle = preparedBuffers.stream()
                .collect(java.util.stream.Collectors.toMap(binding -> binding.handle().handleId(), binding -> binding));
        List<OpenClPreparedArgumentBinding> preparedArguments = plan.argumentBindings().stream()
                .map(binding -> {
                    if (binding.bufferBinding() != null) {
                        String handleId = registry.acquire(binding.bufferBinding()).handleId();
                        OpenClPreparedBufferBinding preparedBuffer = preparedBufferByHandle.get(handleId);
                        return OpenClPreparedArgumentBinding.forBuffer(binding.parameterIndex(), preparedBuffer);
                    }

                    return OpenClPreparedArgumentBinding.forScalar(binding.parameterIndex(), binding.scalarBinding());
                })
                .toList();

        return new OpenClPreparedExecution(
                compiledKernel,
                preparedBuffers,
                plan.scalarBindings(),
                preparedArguments
        );
    }
}
