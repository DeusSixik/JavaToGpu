# API Overview

This page is the fastest way to understand the public JavaToGpu API surface.

## Main Packages

- `net.sixik.ga_utils.javatogpu.api`
  Kernel-facing types such as `GPU`, pointer wrappers, vector wrappers, image wrappers, sampler wrappers, unsigned aliases, and address-space pointer views.
- `net.sixik.ga_utils.javatogpu.api.annotations`
  Source-level annotations such as `@GPU`, `@GPUGlobal`, `@GPUStruct`, `@CCode`, `@GPUIntrinsic`, and related OpenCL metadata annotations.
- `net.sixik.ga_utils.javatogpu.runtime`
  Runtime backend selection, execution scopes, descriptors, and explicit invocation APIs.

## Core Annotations

- `@GPU`
  Marks a Java method as a kernel entry point.
- `@GPUGlobal`, `@GPUConstant`, `@GPULocal`
  Declare the OpenCL address space for array and pointer-like parameters.
- `@GPUStruct`
  Marks a Java class as an ABI-marshalled OpenCL struct.
- `@CCode`
  Declares a reusable GPU helper method.
- `@CCodeLibrary`
  Groups reusable helper methods into a shared helper library.
- `@GPUIntrinsic`
  Declares a method that lowers to a backend intrinsic rather than a normal helper call.
- `@GPUConstantData`, `@GPUExternConstantData`
  Declare constant-data inputs for kernels.
- `@OpenCLAttributes`
  Attaches supported OpenCL attributes to kernels or structs.
- `@OpenCLQualifiers`
  Attaches low-level pointer qualifiers for explicit OpenCL signatures.

## `GPU.*` Builtins

`GPU` is the main kernel builtin facade.

Main groups:

- work-item indexing: `get_global_id`, `get_local_id`, `get_group_id`, `get_global_size`, `get_local_size`
- math: `sin`, `cos`, `tan`, `sqrt`, `pow`, `exp`, `log`, `clamp`, `mix`, `smoothstep`, and many additional scalar/vector OpenCL-style helpers
- synchronization: `barrier`, memory fence constants, local-memory helpers
- images and samplers: image reads, writes, metadata queries, and sampler-aware overloads
- pointer/view bridging: `GPU.global(...)`, `GPU.constant(...)`, `GPU.local(...)`

Use `GPU.*` when you want stable backend-recognized operations instead of ordinary Java calls.

## Value Types

## Scalars

Supported scalar shapes include:

- Java primitives used by the current subset: `byte`, `short`, `int`, `long`, `float`, `double`, `char`
- unsigned aliases: `UByte`, `UShort`, `UInt`, `ULong`

## Pointer Wrappers

Mutable helper-oriented pointer wrappers include families such as:

- `BytePtr`, `CharPtr`, `ShortPtr`, `IntPtr`, `LongPtr`, `FloatPtr`, `DoublePtr`

These are useful for helper mutation patterns like `ptr.value = ...`.

## Address-Space Pointer Views

Address-space-aware views are used for packed/blob-style OpenCL authoring.

Examples:

- `GlobalBytePtr`
- `GlobalIntPtr`
- `GlobalFloatPtr`
- `ConstantBytePtr`
- `LocalFloatPtr`

These support explicit view-style operations such as `add(...)`, `sub(...)`, and `as*Ptr()` where exposed.

## Vector Wrappers

JavaToGpu exposes OpenCL-style vector wrapper families.

Examples:

- `Float2`, `Float3`, `Float4`
- `Int2`, `Int3`, `Int4`
- `UInt2`, `UInt3`, `UInt4`
- `Double2`, `Double3`, `Double4`

Vectors are valid as locals, helper parameters/returns, kernel parameters, and buffer element types.

## Structs

Use `@GPUStruct` for ABI-marshalled OpenCL structs.

Supported field categories:

- primitive scalar fields
- vector fields
- nested `@GPUStruct` fields

Not supported:

- arrays inside struct fields

## Images And Samplers

The API includes wrapper types for OpenCL image and sampler parameters.

Typical usage areas:

- read-only and write-only image kernel parameters
- sampler arguments
- image metadata and pixel access through `GPU.*`

## Runtime API

The main runtime entry point is `GpuRuntime`.

Common usage patterns:

- `GpuRuntime.useOpenCl()`
- `GpuRuntime.useOpenClSharedCache()`
- `GpuRuntime.use(policy)`
- `GpuRuntime.trySelect(policy)`

Execution helpers include:

- `GpuRuntime.invoke(...)`
- `GpuExecutionConfig.oneDimensional(...)`
- `GpuExecutionConfig.twoDimensional(...)`
- generated launcher entry points for rewritten `@GPU` methods

## Programmatic Compiler API

If JavaToGpu is used as a backend from another compiler pipeline, use `GpuProgramCompiler`.

Main entry points:

- `GpuProgramCompiler.createDefault()`
- `compileSource(...)`
- `compileStructuredAsm(...)`

Use the source frontend for normal Java kernels and the structured ASM frontend for intentionally generated canonical bytecode.

## Recommended Reading Order

- [Getting Started](Getting-Started.md)
- [Language Contract](Language-Contract.md)
- [Runtime Guide](Runtime-Guide.md)
- [OpenCL Data Model](OpenCL-Data-Model.md)
- [Cookbook](Cookbook.md)
