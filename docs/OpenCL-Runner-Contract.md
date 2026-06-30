# OpenCL Runner Contract

This page describes the expected contract for future Intel, NVIDIA, and AMD validation runners.

## Required Machine Traits

- Windows host
- working OpenCL ICD/runtime
- Java 17
- enough disk space for Gradle caches and OpenCL report artifacts

## Expected Labels

- `self-hosted`
- `windows`
- `opencl`
- vendor-specific label such as `intel`, `nvidia`, or `amd`

## Expected Task Set

The runner should be able to execute:

1. `:processor:openClVendorValidation`
2. `:processor:integrationOpenClSmokeTest`
3. `:processor:openClWorkloadValidationTest`
4. `:processor:openClLongRunningStabilityTest`
5. `:processor:benchmarkTest`
6. `:processor:openClValidationReport`

or the combined routine:

- `:processor:openClOperationalRoutine`

## Expected Artifact Bundle

Upload the full `processor/build/reports/opencl/` directory.

## Failure Recording Rule

If a lane fails:

- keep the artifact bundle
- record the failing bucket
- capture device/vendor/driver/runtime strings
- add confirmed vendor-specific deviations to the quirks registry
