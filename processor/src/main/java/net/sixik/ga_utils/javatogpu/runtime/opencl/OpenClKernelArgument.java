package net.sixik.ga_utils.javatogpu.runtime.opencl;

public sealed interface OpenClKernelArgument permits OpenClArrayArgument, OpenClScalarArgument {

    OpenClArgumentKind kind();
}
