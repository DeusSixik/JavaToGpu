# Language Contract

JavaToGpu is not a general Java-to-GPU translator.

It supports a restricted Java subset designed for predictable OpenCL code generation.

## Supported Entry Points

- `void` `@GPU` methods
- scalar parameters
- vector wrapper parameters
- `@GPUStruct` value parameters
- annotated array parameters such as `@GPUGlobal float[]`
- image and sampler wrapper parameters

## Supported Control Flow

- `if / else`
- `for`
- `while`
- `do-while`
- `switch / case / default`
- `break`
- `continue`

## Supported Expressions

- arithmetic operators
- comparisons
- logical operators
- bitwise operators on integral scalar expressions
- scalar casts between supported scalar types
- ternary expressions
- array access
- struct and vector field access
- `GPU.*` intrinsics
- `@CCode` helpers
- `@GPUIntrinsic` helpers

## Supported Types

- primitive scalars
- unsigned scalar aliases: `UByte`, `UShort`, `UInt`, `ULong`
- pointer wrappers like `FloatPtr`, `DoublePtr`
- address-space pointer views such as `GlobalBytePtr`
- vector wrappers
- `@GPUStruct` values

## Intentionally Unsupported

- non-`void` `@GPU` entry methods
- arbitrary Java object allocation
- arbitrary Java method calls
- exceptions
- object arrays as a general language feature
- arrays inside `@GPUStruct` fields
- general union-style source authoring

## Related Documents

- [Known Limitations](Known-Limitations.md)
- [OpenCL Data Model](OpenCL-Data-Model.md)
- [Troubleshooting](Troubleshooting.md)
