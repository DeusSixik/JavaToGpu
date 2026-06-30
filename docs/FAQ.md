# FAQ

## Can JavaToGpu run arbitrary Java code on the GPU?

No. It supports a restricted Java subset designed for OpenCL generation.

## Is CUDA supported?

Not yet. OpenCL is the current primary backend.

## Can I return a value from `@GPU` methods?

Not in the current contract. Use output buffers.

## Can I put arrays inside `@GPUStruct`?

No. Keep arrays as kernel parameters or use packed blob schemas.

## Can I feed arbitrary JVM bytecode into the ASM frontend?

No. The ASM frontend expects a GPU-friendly canonical subset.

## How do I deal with optional GPU execution?

Use runtime selection and fallback policies.
