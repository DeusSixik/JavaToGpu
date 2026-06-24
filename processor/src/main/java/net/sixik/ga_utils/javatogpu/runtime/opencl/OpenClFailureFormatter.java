package net.sixik.ga_utils.javatogpu.runtime.opencl;

import net.sixik.ga_utils.javatogpu.runtime.GpuKernelDescriptor;

final class OpenClFailureFormatter {

    private OpenClFailureFormatter() {
    }

    static IllegalStateException buildFailure(GpuKernelDescriptor descriptor, String deviceLabel, Throwable cause) {
        return new IllegalStateException(
                "OpenCL kernel build failed for kernel "
                        + descriptor.kernelName()
                        + " on device "
                        + deviceLabel
                        + " ["
                        + descriptor.kernelResource()
                        + "]: "
                        + rootMessage(cause)
                        + "; check the generated kernel source and enable ABI debug for layout-sensitive failures",
                cause
        );
    }

    static IllegalStateException executionFailure(GpuKernelDescriptor descriptor, String deviceLabel, Throwable cause) {
        return new IllegalStateException(
                "OpenCL kernel execution failed for kernel "
                        + descriptor.kernelName()
                        + " on device "
                        + deviceLabel
                        + ": "
                        + rootMessage(cause),
                cause
        );
    }

    static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        String message = current == null ? "Unknown OpenCL failure" : current.getMessage();
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                message = current.getMessage();
            }
            current = current.getCause();
        }
        if (message == null || message.isBlank()) {
            return throwable == null ? "Unknown OpenCL failure" : throwable.getClass().getSimpleName();
        }
        return message;
    }
}
