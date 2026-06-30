# Troubleshooting

Use this page as the first stop for common JavaToGpu failures.

## Unknown `@CCode` Helper

Typical message:

- `Unknown @CCode helper call in @GPU method: Helpers.foo`

Usually means:

- helper owner mismatch
- helper not included in the compilation/input set
- signature mismatch at the call site

## Unsupported GPU Parameter Type

Typical fix:

- add `@GPUGlobal`, `@GPUConstant`, or `@GPULocal` for arrays
- use supported wrappers for pointer/vector/struct-like values

## Unsupported `@GPUStruct` Field

Typical fix:

- use primitive fields, vector fields, or nested structs
- move arrays out of structs into kernel parameters

## ABI Or Readback Failure

Typical fix:

- keep struct fields ABI-safe
- use `@GPUStruct[]` for struct buffers
- use vector wrapper arrays for vector buffers
- add accessible no-arg constructors for readback reconstruction when required

## OpenCL Build Failure

Suggested order:

1. inspect generated OpenCL source
2. enable ABI debug with `javatogpu.opencl.debugAbi=true`
3. check capability gates such as `DOUBLE_PRECISION` or `IMAGES`
4. compare the failure against [Device Quirks](Device-Quirks.md)

## Vendor-Specific Issue

If the same failure is isolated to one device stack, treat it as a candidate device quirk and record it once reproduced.
