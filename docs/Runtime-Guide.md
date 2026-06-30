# Runtime Guide

JavaToGpu runtime execution is controlled through `GpuRuntime`.

## Runtime Modes

### Isolated OpenCL Backend

```java
try (GpuRuntimeScope ignored = GpuRuntime.useOpenCl()) {
    Demo.kernel(input, output);
}
```

### Shared OpenCL Cache

```java
try (GpuRuntimeScope ignored = GpuRuntime.useOpenClSharedCache()) {
    Demo.kernel(input, output);
} finally {
    GpuRuntime.shutdownOpenClSharedCache();
}
```

### Fallback Policy

```java
GpuRuntimeBackendPolicy policy = GpuRuntimeBackendPolicy.builder()
        .preferOpenClSharedCache()
        .preferFactory(MyCpuFallbackBackend::new)
        .build();

try (GpuRuntimeScope ignored = GpuRuntime.use(policy)) {
    Demo.kernel(input, output);
}
```

### Precheck And Skip

```java
GpuRuntimeSelectionResult result = GpuRuntime.trySelect(policy);
if (!result.matched()) {
    return;
}
try (GpuRuntimeScope ignored = result.install()) {
    Demo.kernel(input, output);
}
```

## Explicit Work Sizes

For packed blob workloads where logical item count does not match raw buffer length:

```java
GpuGeneratedLauncherInvoker.invokeWithGlobalWorkSize(
        OwnerClass.class,
        "kernel",
        itemCount,
        blob,
        view,
        output
);
```

Programmatic runtime callers can use:

```java
GpuRuntime.invoke(GpuExecutionConfig.oneDimensional(itemCount), descriptor, blob, view, output);
```

2D launches are supported through:

- `GpuExecutionConfig.twoDimensional(globalX, globalY)`
- `GpuExecutionConfig.twoDimensional(globalX, globalY, localX, localY)`

## ABI Debug

Enable ABI diagnostics with:

```java
System.setProperty("javatogpu.opencl.debugAbi", "true");
```

## Related Documents

- [Validation and Operations](Validation-and-Operations.md)
- [Troubleshooting](Troubleshooting.md)
- [OpenCL Data Model](OpenCL-Data-Model.md)
