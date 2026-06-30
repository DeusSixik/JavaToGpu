# Cookbook

## Pointer Helper Pattern

```java
@CCode
static void setValue(FloatPtr ptr) {
    ptr.value = 42.0f;
}
```

## Packed Blob Read

```java
GlobalBytePtr root = GPU.global(blob);
int value = root.add(view.offset + id * 4).asIntPtr().value;
```

## Inline Helper

```java
@CCode(inline = true)
static float lerp(float a, float b, float t) {
    return a + (b - a) * t;
}
```

## Struct Usage

```java
@GPUStruct
static final class Point {
    public float x;
    public float y;
}
```

## Fallback Runtime Flow

```java
GpuRuntimeSelectionResult result = GpuRuntime.trySelect(policy);
if (result.matched()) {
    try (GpuRuntimeScope ignored = result.install()) {
        Demo.kernel(input, output);
    }
}
```

## Image Workflow

```java
try (OpenClGpuRuntimeBackend backend = new OpenClGpuRuntimeBackend();
     Image2DReadOnly input = backend.createReadOnlyRgbaIntImage(2, 1, pixels);
     Image2DWriteOnly output = backend.createWriteOnlyRgbaFloatImage(2, 1);
     Sampler sampler = backend.createNearestClampToEdgeSampler()) {
    ImageKernel.run(input, output, sampler, sums);
}
```
