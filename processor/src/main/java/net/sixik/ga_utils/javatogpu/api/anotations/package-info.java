/**
 * Annotations that describe how Java code should be translated to GPU code.
 *
 * <p>Most user code interacts with these annotations directly:
 *
 * <ul>
 *     <li>{@link net.sixik.ga_utils.javatogpu.api.anotations.GPU} marks a kernel entry method.</li>
 *     <li>{@link net.sixik.ga_utils.javatogpu.api.anotations.CCode} marks helper methods callable from kernels.</li>
 *     <li>{@link net.sixik.ga_utils.javatogpu.api.anotations.GPUGlobal},
 *         {@link net.sixik.ga_utils.javatogpu.api.anotations.GPUConstant} and
 *         {@link net.sixik.ga_utils.javatogpu.api.anotations.GPULocal} declare kernel parameter address spaces.</li>
 *     <li>{@link net.sixik.ga_utils.javatogpu.api.anotations.GPUStruct} marks Java classes that should be emitted as OpenCL structs.</li>
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * @GPU
 * static void kernel(
 *         @GPUGlobal float[] input,
 *         @GPUConstant float[] lookup,
 *         @GPULocal float[] scratch,
 *         @GPUGlobal float[] output
 * ) {
 *     int id = GPU.get_global_id(0);
 *     int lid = GPU.get_local_id(0);
 *     scratch[lid] = input[id] + lookup[lid];
 *     GPU.barrier(GPU.CLK_LOCAL_MEM_FENCE);
 *     output[id] = scratch[lid];
 * }
 * }</pre>
 */
package net.sixik.ga_utils.javatogpu.api.anotations;
