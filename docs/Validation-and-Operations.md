# Validation and Operations

This page defines the practical production-style validation flow for JavaToGpu.

## Main Validation Buckets

- `:processor:openClVendorValidation`
- `:processor:integrationOpenClSmokeTest`
- `:processor:openClWorkloadValidationTest`
- `:processor:openClLongRunningStabilityTest`
- `:processor:benchmarkTest`
- `:processor:openClValidationReport`

## Recommended Full Routine

```powershell
./gradlew.bat :processor:openClOperationalRoutine --console=plain
```

## Generated OpenCL Report Artifacts

The runtime report bundle lives under `processor/build/reports/opencl/`.

Important files:

- `validation-report.md`
- `validation-history.md`
- `bucket-status.properties`
- `workload-summary.properties`
- `long-running-summary.properties`

## What The Bundle Proves

- which validation buckets were actually executed
- current runtime/device snapshot
- serious workload CPU-vs-GPU equivalence status
- long-running ABI/resource stability status
- rolling local validation history

## Vendor Strategy

Current target vendors:

- Intel
- NVIDIA
- AMD

Until all three are covered on real machines, treat the validated vendor set as partial production evidence, not universal proof.

## Runner Contract

For future self-hosted validation machines, see the runner contract in [OpenCL Runner Contract](OpenCL-Runner-Contract.md).

Vendor-specific runtime deviations should be recorded in [Device Quirks](Device-Quirks.md).
