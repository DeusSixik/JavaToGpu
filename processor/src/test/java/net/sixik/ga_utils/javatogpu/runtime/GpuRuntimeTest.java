package net.sixik.ga_utils.javatogpu.runtime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GpuRuntimeTest {

    @Test
    void defaultBackendThrowsHelpfulError() {
        GpuKernelDescriptor descriptor = new GpuKernelDescriptor(
                "kernel",
                "javatogpu/sample/Demo/kernel.cl",
                "__kernel void kernel() {}",
                java.util.List.of()
        );

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> GpuRuntime.defaultBackend().invoke(new GpuKernelInvocation(descriptor, new Object[0]))
        );

        assertEquals("GPU runtime backend is not configured for kernel kernel", exception.getMessage());
    }
}
