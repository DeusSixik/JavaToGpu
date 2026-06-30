# OpenCL Data Model

This page documents the most important low-level JavaToGpu data-model concepts.

## Constant Data

JavaToGpu supports two constant-data forms:

- embedded constant tables via `@GPUConstantData`
- external constant declarations via `@GPUExternConstantData`

These currently support primitive scalar arrays only.

## Packed Blob Views

Packed blob workflows use:

- a root `@GPUGlobal byte[]`
- a small `@GPUStruct` offset schema
- typed reads through `GlobalBytePtr.add(...).as*Ptr().value`

Example pattern:

```java
GlobalBytePtr root = GPU.global(blob);
int value = root.add(view.offset + id * 4).asIntPtr().value;
```

## Typed Address-Space Pointers

The current pointer/view model supports:

- `global`
- `constant`
- `local`

with typed wrapper families for primitive scalar pointees.

## Struct Rules

`@GPUStruct` supports:

- primitive scalar fields
- vector fields
- nested `@GPUStruct` values
- supported OpenCL attributes like `packed` and `aligned(...)`

It does not support arrays inside struct fields.

## Reinterpretation Policy

JavaToGpu does not expose general source-level union authoring.

Use the typed pointer/view model instead of arbitrary overlapping storage semantics.

## Related Documents

- [Language Contract](Language-Contract.md)
- [Cookbook](Cookbook.md)
- [Known Limitations](Known-Limitations.md)
