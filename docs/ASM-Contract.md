# ASM Contract

JavaToGpu includes a structured ASM frontend.

This frontend is not a general JVM decompiler. It expects a GPU-friendly canonical bytecode subset.

## Main Rule

The correct architecture is:

`your AST -> GPU-friendly ASM -> JavaToGpu ASM frontend -> IR -> OpenCL`

Not:

`arbitrary JVM bytecode -> hope JavaToGpu can reconstruct everything`

## Expected Bytecode Style

- static methods only
- predictable control-flow graphs
- `INVOKESTATIC` calls only
- explicit temporaries preferred over stack tricks
- whitelist object construction only for supported wrappers/types

## Best Practices

- keep operand stack shallow
- store intermediate values in locals
- avoid virtual dispatch
- normalize loops to canonical forms
- normalize math calls to `GPU.*` or stable helper owners

## Not Supported

- arbitrary JVM bytecode
- `invokevirtual`
- `invokeinterface`
- `invokedynamic`
- exception tables
- general object semantics

## When to Use ASM

Use the ASM path when:

- you already own an AST or IR
- you want programmatic kernel generation
- you need lower-level control than the source frontend provides

For ordinary project code, prefer the Java source frontend.
