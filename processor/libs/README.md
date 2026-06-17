# Packager

Pure Java 17 low-level packer for moving large data batches from Java to native code without JNI-side serialization.

## What is implemented

- `bytepack.FastPackager` packs arbitrary `byte[]` into a 64-byte aligned off-heap buffer.
- Blocks are either raw or LZ4-raw compatible. In C, raw blocks are copied and LZ4 blocks can be decoded by `LZ4_decompress_safe`.
- `floatgrid.FloatGridPacker` is a lossy predictor + varint codec for smooth `float` grids such as Perlin/simplex noise.
- `gpu.GpuFloatPacker` writes headerless fixed-width upload payloads: `FLOAT32`, `FLOAT16`, `UNORM16`, `SNORM16`, `UNORM8`.
- `opencl.core` wraps LWJGL/OpenCL device/context/queue/program/kernel/buffer boilerplate for reusable pipelines.
- `opencl.pipeline.TripleBufferedOpenClPipeline` overlaps pinned upload, unmap/transfer, and compute with event-based slot reuse.
- `layout.BufferLayout` describes packed binary layouts and can emit matching OpenCL offset headers.
- `compute.ir` provides a domain-neutral scalar field IR and an OpenCL C generator for GPU-side field generation.
- `opencl.cache.DeviceBufferPool` keeps reusable device-resident buffers for static tables/tiles.
- `opencl.staging.PinnedMappedBufferPool` reuses `CL_MEM_ALLOC_HOST_PTR` mapped upload slots.
- `transport.DirtyRangeSet` + `UploadPlanner` decide when a partial dirty upload beats a full upload.
- `transport.DirtyUpdateJournal*` serializes sparse CPU->GPU update commands.
- `transport.CompactJournal*` reads compact GPU->CPU journals.
- `opencl.pipeline.OpenClUploadExecutor` and `OpenClDownloadExecutor` enqueue sparse range transfers.
- `opencl/transport_kernels.cl` applies dirty update journals on GPU and appends compact GPU-side journals.
- `opencl.transport.OpenClTransportKernels` provides typed launch helpers for those transport kernels.
- `opencl.transport.TransportSession` ties together queues, resident buffers, pinned staging, sparse uploads, and journal kernels behind one integration facade.
- `profiler.TransferCostModel` estimates raw vs packed vs GPU-generated upload paths.
- `autotune.TransportAutotuner` chooses resident/full/sparse/packed/generated transfer strategies.
- `opencl/gpu_primitives.cl` contains generic threshold, compaction, block reduction, fill, and hash kernels.
- `buffer.AlignedNativeBuffer` owns aligned native memory and exposes `address()` and `size()` for native hand-off.

## Packages

- `dev.denismasterherobrine.packager.buffer` - off-heap allocation and primitive memory I/O.
- `dev.denismasterherobrine.packager.bytepack` - generic byte frame, LZ4-compatible block codec, unpack API.
- `dev.denismasterherobrine.packager.floatgrid` - Perlin/noise-oriented float grid codec.
- `dev.denismasterherobrine.packager.gpu` - OpenCL/GPU-oriented fixed-width float payloads.
- `dev.denismasterherobrine.packager.opencl.core` - LWJGL/OpenCL device/context/program/kernel/buffer helpers.
- `dev.denismasterherobrine.packager.opencl.pipeline` - reusable event-driven triple buffering.
- `dev.denismasterherobrine.packager.opencl.cache` - simple LRU pool for device-resident OpenCL buffers.
- `dev.denismasterherobrine.packager.opencl.staging` - reusable pinned mapped upload slots.
- `dev.denismasterherobrine.packager.opencl.transport` - OpenCL launch helpers for dirty and compact journals.
- `dev.denismasterherobrine.packager.opencl.lwjgl` - LWJGL/OpenCL full pipeline benchmark.
- `dev.denismasterherobrine.packager.transport` - dirty ranges and full-vs-sparse upload planning.
- `dev.denismasterherobrine.packager.autotune` - transport strategy selection helpers.
- `dev.denismasterherobrine.packager.layout` - declarative binary schemas and OpenCL layout header generation.
- `dev.denismasterherobrine.packager.compute.ir` - scalar field IR and OpenCL kernel source generator.
- `dev.denismasterherobrine.packager.profiler` - transfer path cost model and decision helpers.
- `dev.denismasterherobrine.packager.section` - SectionDescriptor-like raw/packed GPU upload layouts.
- `dev.denismasterherobrine.packager.config` - packer tuning presets.
- `dev.denismasterherobrine.packager.bench` - simple local benchmark entry point.

## Generic byte frame

All integers are little-endian.

Frame header is 32 bytes:

| Offset | Type | Meaning |
| --- | --- | --- |
| 0 | `u32` | magic `PCKR` |
| 4 | `u8` | version, currently `1` |
| 5 | `u8` | frame kind, `1` = block stream |
| 6 | `u16` | flags, currently `0` |
| 8 | `u32` | original byte length |
| 12 | `u32` | block size |
| 16 | `u32` | block count |
| 20 | `u32` | first payload offset |
| 24 | `u32` | total frame size |
| 28 | `u32` | reserved |

Each block header is 16 bytes and starts at `32 + blockIndex * 16`:

| Offset | Type | Meaning |
| --- | --- | --- |
| 0 | `u32` | decoded block length |
| 4 | `u32` | stored block length |
| 8 | `u32` | payload offset from frame start |
| 12 | `u8` | codec: `0` raw, `1` LZ4 raw block |
| 13 | `u8` | flags, currently `0` |
| 14 | `u16` | reserved |

## Usage

```java
import dev.denismasterherobrine.packager.bytepack.FastPackager;
import dev.denismasterherobrine.packager.bytepack.PackedFrame;

byte[] source = ...;

try (PackedFrame frame = FastPackager.pack(source)) {
    long address = frame.address();
    long size = frame.size();
    // Pass address + size to native code through your engine boundary.
}
```

Hot path with reusable native memory:

```java
import dev.denismasterherobrine.packager.buffer.AlignedNativeBuffer;
import dev.denismasterherobrine.packager.bytepack.FastPackager;
import dev.denismasterherobrine.packager.bytepack.PackedFrame;
import dev.denismasterherobrine.packager.config.PackConfig;

FastPackager packager = new FastPackager(PackConfig.FAST);
try (AlignedNativeBuffer buffer = AlignedNativeBuffer.allocateShared(FastPackager.maxPackedLength(source.length))) {
    PackedFrame frame = packager.packInto(source, 0, source.length, buffer);
    nativeSubmit(frame.address(), frame.size());
}
```

For Perlin-like float fields:

```java
import dev.denismasterherobrine.packager.floatgrid.FloatGridPacker;
import dev.denismasterherobrine.packager.floatgrid.PackedFloatGrid;

float[] noise = ...;

try (PackedFloatGrid grid = FloatGridPacker.pack(noise, width, height)) {
    long address = grid.address();
    long size = grid.size();
}
```

For OpenCL/GPU uploads, prefer headerless fixed-width payloads when possible:

```java
import dev.denismasterherobrine.packager.gpu.GpuFloatFormat;
import dev.denismasterherobrine.packager.gpu.GpuFloatPacker;
import dev.denismasterherobrine.packager.gpu.PackedGpuFloatBuffer;

float[] heights = ...;

try (PackedGpuFloatBuffer packed =
             GpuFloatPacker.pack(heights, 0, heights.length, GpuFloatFormat.UNORM16, -1.0f, 1.0f)) {
    // Upload packed.address(), packed.size().
    // Kernel args also need packed.min() and packed.scale() for UNORM decode.
}
```

If an OpenCL binding gives you a mapped/pinned pointer, pack directly into that address to avoid the extra staging copy:

```java
long mappedAddress = ...;

GpuFloatPayload payload = GpuFloatPacker.packIntoAddress(
        heights, 0, heights.length, GpuFloatFormat.UNORM16, -1.0f, 1.0f, mappedAddress
);
```

For large mapped buffers, use the parallel packer:

```java
GpuFloatPayload payload = GpuFloatPacker.packIntoAddressParallel(
        heights, 0, heights.length, GpuFloatFormat.UNORM16, -1.0f, 1.0f, mappedAddress
);
```

Generic OpenCL setup:

```java
import dev.denismasterherobrine.packager.opencl.core.OpenClContext;
import dev.denismasterherobrine.packager.opencl.core.OpenClDevice;
import dev.denismasterherobrine.packager.opencl.core.OpenClDevices;
import dev.denismasterherobrine.packager.opencl.core.OpenClProgram;

OpenClDevice device = OpenClDevices.selectBest();
try (OpenClContext context = OpenClContext.create(device);
     OpenClProgram program = context.buildProgram(kernelSource)) {
    // Create queues, kernels, buffers, or a TripleBufferedOpenClPipeline.
}
```

High-level OpenCL transport session:

```java
import dev.denismasterherobrine.packager.opencl.core.OpenClBuffer;
import dev.denismasterherobrine.packager.opencl.core.OpenClDevice;
import dev.denismasterherobrine.packager.opencl.core.OpenClDevices;
import dev.denismasterherobrine.packager.opencl.transport.TransportSession;
import dev.denismasterherobrine.packager.opencl.transport.TransportStagedUpload;
import org.lwjgl.system.MemoryStack;

OpenClDevice device = OpenClDevices.selectBest();

try (TransportSession session = TransportSession.create(device)
        .devicePoolBytes(1024L * 1024L * 1024L)
        .stagingSlots(3)
        .build()) {
    OpenClBuffer resident = session.readWriteBuffer("density-slots", bytes);

    try (MemoryStack stack = MemoryStack.stackPush();
         TransportStagedUpload upload = session.stageUpload(bytes, (mapped, address) -> {
             // Pack directly into pinned mapped OpenCL memory.
             packIntoAddress(address);
         })) {
        kernel.setArg(0, upload.buffer())
                .setArg(1, resident);
        long kernelEvent = kernel.enqueue1D(session.computeQueue(), workItems, upload.waitList(stack));
        upload.completeWith(kernelEvent); // hands slot lifetime from unmap event to kernel event
    }
}
```

If an existing runtime already owns OpenCL handles, wrap them without transferring ownership:

```java
TransportSession session = TransportSession
        .wrap(deviceInfo, existingContextHandle, existingQueueHandle)
        .build();
// session.close() will not release the wrapped context/queue handles.
```

Plan sparse dirty uploads before touching PCIe:

```java
import dev.denismasterherobrine.packager.transport.DirtyRangeSet;
import dev.denismasterherobrine.packager.transport.UploadPlan;
import dev.denismasterherobrine.packager.transport.UploadPlanner;

DirtyRangeSet dirty = new DirtyRangeSet()
        .addAligned(128 * 1024, 4096, 64)
        .addAligned(2 * 1024 * 1024, 8192, 64);

UploadPlan plan = UploadPlanner.conservative().plan(totalBytes, dirty);
// plan.mode() is NONE, SPARSE, or FULL. Use OpenClUploadExecutor for SPARSE/FULL.
```

The same plan can be submitted through `TransportSession`:

```java
long uploadEvent = session.enqueueUpload(deviceBuffer, sourceByteBuffer, plan, 0L);
```

Use compact journals when GPU should return only changed/candidate records:

```java
import dev.denismasterherobrine.packager.transport.CompactJournalLayout;
import dev.denismasterherobrine.packager.transport.CompactJournalReader;

long readbackBytes = CompactJournalLayout.bytes(maxRecords, recordStride);
// GPU writes the journal header + records; CPU reads only the compact result.
var summary = CompactJournalReader.summary(mappedAddress);
```

GPU-side transport kernels can apply dirty journals or append compact results without reading dense buffers back to CPU:

```java
import dev.denismasterherobrine.packager.opencl.transport.OpenClTransportKernels;

String source = OpenClTransportKernels.source();
// Build a program, create pk_apply_dirty_update_journal_u32 or pk_compact_journal_append_index_u32,
// then call the matching OpenClTransportKernels.enqueue... helper.
```

Generate an OpenCL field kernel instead of uploading dense samples:

```java
import dev.denismasterherobrine.packager.compute.ir.ExpressionArena;
import dev.denismasterherobrine.packager.compute.ir.FieldOutput;
import dev.denismasterherobrine.packager.compute.ir.OpenClExpressionCompiler;
import dev.denismasterherobrine.packager.compute.ir.OpenClKernelSource;

ExpressionArena ir = new ExpressionArena();
var density = ir.clamp(
        ir.add(ir.valueNoise3(123, 0.05f), ir.mul(ir.y(), ir.constant(0.001f))),
        ir.constant(-1.0f),
        ir.constant(1.0f)
);

OpenClKernelSource source = OpenClExpressionCompiler.compile2D(
        "generate_density",
        java.util.List.of(new FieldOutput("density", density))
);
```

The IR is intentionally generic and not tied to Minecraft. It supports:

- 1D/2D/3D kernels through `compile1D`, `compile2D`, and `compile3D`.
- Coordinates, constants, uniform float parameters, and 1D float lookup tables.
- Arithmetic, min/max, power/modulo/atan2, comparisons, select, clamp, mix, smoothstep, and range choice.
- Unary math such as sin/cos/tan, sqrt/rsqrt, exp/log, floor/ceil/fract, saturate, and sign.
- Hash value noise and fractal value noise as baseline procedural primitives.
- CPU evaluation through `ScalarExpressionEvaluator` for fallback and validation.

## Performance notes

- Maximum compression and maximum throughput are conflicting goals. This project uses a fast path first: cheap LZ4-style byte packing, plus a domain-specific float-grid transform when the data is smooth.
- For GPU transfer bottlenecks, fixed-width quantization often beats stream compression: it is parallel to decode, predictable for kernels, and cuts PCIe bytes by 2x with `FLOAT16`/`UNORM16` or 4x with `UNORM8`.
- For OpenCL, the lowest-copy path is usually: allocate/map pinned host memory, call `GpuFloatPacker.packIntoAddress(...)`, enqueue an async copy or unmap, then overlap the next CPU pack with GPU work.
- Large fixed-width GPU float packs automatically use the parallel writer through the high-level `packInto(...)` API; explicit `packIntoAddressParallel(...)` is still available for mapped pointers.
- `opencl/lwjgl/LwjglOpenClPipelineBenchmark` measures the painful path directly: map pinned OpenCL buffer -> pack into mapped address -> unmap/transfer -> decode kernel -> finish.
- `FloatGridPacker` gives a better byte ratio on smooth fields, but its predictor/varint stream is sequential; it is better for CPU/native transfer than direct GPU kernels.
- If data can be regenerated on GPU, send seeds/parameters instead of samples. For Perlin/simplex noise this is usually the physical lower bound for transfer: almost zero payload.
- Use `PackConfig.FAST` for throughput, `PackConfig.BALANCED` as a middle ground, and `PackConfig.DENSE` when byte compression ratio matters more than CPU time.
- If the data is already high entropy, `FastPackager` stores raw blocks to avoid wasting C-side decode time. The `FAST` path also samples blocks to skip LZ4 work when repeated sequences are unlikely.
- Keep buffers reusable in hot paths: allocate `AlignedNativeBuffer` once with `FastPackager.maxPackedLength(...)`, then call `packInto(...)`.
- The Java 17 backend uses `sun.misc.Unsafe` for off-heap allocation and copies. Java 25+ may print deprecation warnings for Unsafe, but the bytecode target remains Java 17.
- `allocateShared(...)` is kept as a semantic hint for async/native ownership; with Unsafe memory it maps to the same off-heap allocator as `allocate(...)`.

Run tests:

```shell
./gradlew test
```

Run quick local benchmark:

```shell
./gradlew classes
java -cp build/classes/java/main dev.denismasterherobrine.packager.bench.BenchmarkMain
```

Run LWJGL/OpenCL full pipeline benchmark:

```shell
./gradlew openclPipelineBenchmark
./gradlew openclPipelineBenchmark --args="1024 1024"
```

Run IR generation benchmark:

```shell
./gradlew irOpenClBenchmark
./gradlew irOpenClBenchmark --args="2048 2048"
```

Run transport strategy estimator:

```shell
./gradlew transportAutotuneBenchmark
./gradlew transportAutotuneBenchmark --args="4096 0.10 128 24"
```

OpenCL kernels are stored in `src/main/resources/dev/denismasterherobrine/packager/opencl/decode_kernels.cl`.

Run triple-buffered SectionDescriptor-like benchmark:

```shell
./gradlew sectionDescriptorOpenClBenchmark
./gradlew sectionDescriptorOpenClBenchmark --args="32768"
```