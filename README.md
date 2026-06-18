# Java To GPU
JavaToGPU is a lightweight and powerful framework for translating Java code into OpenCL C on the fly.
It allows you to write high-performance compute kernels directly in Java, manipulate pointers, and execute computations on the GPU without writing manual C/C++ code or dealing with complex JNI boilerplate.

## Key Features
- AST Translation: Code is parsed and translated into native OpenCL C right at build time. No code strings - just pure Java.
- Zero-Overhead Intrinsics: Full support for GPU math functions (sin, cos, tan, clamp, etc.) via a transparent API.
- Custom Pointers: Built-in FloatPtr, DoublePtr, and IntPtr types for convenient memory manipulation and passing data by reference in inline methods.
- Hot Swapping: Calls to methods annotated with @GPU are automatically intercepted by a custom classloader and redirected to the GPU.