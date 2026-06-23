package net.sixik.ga_utils.javatogpu.runtime.opencl;

record OpenClRuntimeCapabilities(
        String deviceLabel,
        String deviceVersion,
        boolean supportsDoublePrecision,
        boolean supportsImages,
        boolean supportsImage3dWrites,
        long localMemoryBytes,
        long maxWorkGroupSize
) {
}
