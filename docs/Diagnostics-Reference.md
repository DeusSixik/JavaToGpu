# Diagnostics Reference

This page summarizes the most common JavaToGpu diagnostics.

## Common Compiler Diagnostics

- unknown `@CCode` helper
- ambiguous helper call
- unsupported GPU parameter type
- unsupported `@GPUStruct` field type
- unsupported Java construct in `@GPU` code

## Common Runtime Diagnostics

- unsupported OpenCL upload/readback payload type
- invalid image or sampler handle
- missing capability such as `DOUBLE_PRECISION` or `IMAGES`
- OpenCL build failure on the selected vendor stack

## Related Document

- [Troubleshooting](Troubleshooting.md)
- [Device Quirks](Device-Quirks.md)
