# Device Quirks

This page tracks known vendor-specific or driver-specific OpenCL behavior that affects JavaToGpu.

Record only issues that were reproduced on a real device stack or confirmed by repeated validation failures.

## Entry Template

- Vendor
- Device
- Driver / Runtime
- Platform
- Affected validation bucket
- Symptom
- Minimal reproducer
- Workaround
- Status: `open`, `mitigated`, or `driver-fixed`
- Matching validation artifact

## Current Status

No confirmed vendor-specific quirks are recorded in the public tracker yet.

## Known Clean Baseline

- Vendor: `NVIDIA`
- Device: `NVIDIA GeForce RTX 5070`
- Driver / Runtime: `595.97`
- Platform: `OpenCL 3.0 CUDA 13.2.73`
- Validated buckets: `:processor:openClVendorValidation`, `:processor:integrationOpenClSmokeTest`, `:processor:openClValidationReport`
- Result: no vendor-specific issue observed in the recorded baseline
- Validation artifact: `processor/build/reports/opencl/validation-report.md`

## Recording Rules

- Do not add generic "works on my machine" notes.
- Keep one issue as one clear entry.
- If a driver update fixes the issue, keep the history and mark it `driver-fixed`.
