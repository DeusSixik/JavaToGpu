# Known Limitations

JavaToGpu is a restricted GPU-safe subset compiler, not a general Java-to-GPU runtime.

## Current Limits

- `@GPU` entry methods must currently return `void`.
- Arbitrary Java object allocation is not supported inside GPU code.
- Arbitrary Java method dispatch is not supported inside GPU code.
- Arrays inside `@GPUStruct` fields are not supported by the current OpenCL ABI.
- Some advanced OpenCL features are exposed only through explicit wrappers, qualifiers, or helper patterns.
- The structured ASM frontend expects canonical GPU-friendly bytecode, not arbitrary JVM bytecode.
- CUDA is planned later and is not part of the current production path.

## Practical Rule

If a construct cannot be lowered predictably to OpenCL C and marshalled safely through the current ABI, it should fail early with a diagnostic instead of being accepted implicitly.

## Related Documents

- [Language Contract](Language-Contract.md)
- [ASM Contract](ASM-Contract.md)
- [Troubleshooting](Troubleshooting.md)
- [OpenCL Data Model](OpenCL-Data-Model.md)
