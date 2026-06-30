# Getting Started

## What JavaToGpu Does

JavaToGpu lets you write GPU kernels in a restricted Java subset and compile them to OpenCL C.

The pipeline handles:

- frontend validation
- helper resolution
- OpenCL source generation
- launcher generation
- runtime execution through `GpuRuntime`

## Add the Processor

```groovy
dependencies {
    implementation project(':processor')
    annotationProcessor project(':processor')
}
```

## Minimal Kernel

```java
import net.sixik.ga_utils.javatogpu.api.GPU;
import net.sixik.ga_utils.javatogpu.api.annotations.GPUGlobal;

public final class Demo {

    @net.sixik.ga_utils.javatogpu.api.annotations.GPU
    public static void kernel(
            @GPUGlobal float[] input,
            @GPUGlobal float[] output
    ) {
        int id = GPU.get_global_id(0);
        output[id] = GPU.sin(input[id]) + 2.0f;
    }
}
```

## Execute a Kernel

```java
import net.sixik.ga_utils.javatogpu.runtime.GpuRuntime;
import net.sixik.ga_utils.javatogpu.runtime.GpuRuntimeScope;

try (GpuRuntimeScope ignored = GpuRuntime.useOpenCl()) {
    Demo.kernel(input, output);
}
```

For repeated calls, prefer shared cache:

```java
try (GpuRuntimeScope ignored = GpuRuntime.useOpenClSharedCache()) {
    Demo.kernel(input, output);
} finally {
    GpuRuntime.shutdownOpenClSharedCache();
}
```

## Next Documents

- [API Overview](API-Overview.md)
- [Language Contract](Language-Contract.md)
- [Runtime Guide](Runtime-Guide.md)
- [Cookbook](Cookbook.md)
