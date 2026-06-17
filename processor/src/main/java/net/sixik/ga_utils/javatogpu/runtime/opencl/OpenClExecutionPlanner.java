package net.sixik.ga_utils.javatogpu.runtime.opencl;

import net.sixik.ga_utils.javatogpu.runtime.GpuKernelParameterAccess;

import java.util.ArrayList;
import java.util.List;

public final class OpenClExecutionPlanner {

    private OpenClExecutionPlanner() {
    }

    public static OpenClExecutionPlan plan(OpenClKernelArguments arguments) {
        List<OpenClBufferBinding> bufferBindings = new ArrayList<>();
        List<OpenClScalarBinding> scalarBindings = new ArrayList<>();
        List<OpenClPlannedArgumentBinding> argumentBindings = new ArrayList<>();

        for (int index = 0; index < arguments.values().size(); index++) {
            OpenClKernelArgument argument = arguments.values().get(index);
            if (argument instanceof OpenClArrayArgument arrayArgument) {
                OpenClBufferBinding binding = new OpenClBufferBinding(
                        arrayArgument.kind(),
                        arrayArgument.access(),
                        arrayArgument.sourceArray(),
                        arrayArgument.length(),
                        arrayArgument.access() == GpuKernelParameterAccess.READ_ONLY
                                || arrayArgument.access() == GpuKernelParameterAccess.READ_WRITE,
                        arrayArgument.access() == GpuKernelParameterAccess.READ_WRITE
                );
                bufferBindings.add(binding);
                argumentBindings.add(OpenClPlannedArgumentBinding.forBuffer(index, binding));
                continue;
            }

            if (argument instanceof OpenClScalarArgument scalarArgument) {
                OpenClScalarBinding binding = new OpenClScalarBinding(
                        scalarArgument.kind(),
                        scalarArgument.access(),
                        scalarArgument.value()
                );
                scalarBindings.add(binding);
                argumentBindings.add(OpenClPlannedArgumentBinding.forScalar(index, binding));
                continue;
            }

            throw new IllegalArgumentException("Unsupported marshalled OpenCL argument: " + argument);
        }

        return new OpenClExecutionPlan(
                List.copyOf(bufferBindings),
                List.copyOf(scalarBindings),
                List.copyOf(argumentBindings)
        );
    }
}
