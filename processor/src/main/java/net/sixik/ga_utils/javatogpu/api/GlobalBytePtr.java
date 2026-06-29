package net.sixik.ga_utils.javatogpu.api;

import net.sixik.ga_utils.javatogpu.api.annotations.GPUPointerAddressSpace;
import net.sixik.ga_utils.javatogpu.api.annotations.GPUIntrinsic;
import net.sixik.ga_utils.javatogpu.api.annotations.GPUPointerType;

/**
 * Typed OpenCL {@code __global char*} helper pointer for packed byte-backed blobs.
 */
@GPUPointerType(valueType = "byte", addressSpace = GPUPointerAddressSpace.GLOBAL)
public final class GlobalBytePtr {

    public byte value;

    public GlobalBytePtr() {
    }

    public GlobalBytePtr(byte value) {
        this.value = value;
    }

    @GPUIntrinsic(code = "(({this}) + ({0}))")
    public GlobalBytePtr add(int bytes) {
        return this;
    }

    @GPUIntrinsic(code = "(({this}) - ({0}))")
    public GlobalBytePtr sub(int bytes) {
        return this;
    }

    @GPUIntrinsic(code = "((__global char*) ({this}))")
    public GlobalCharPtr asCharPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__global short*) ({this}))")
    public GlobalShortPtr asShortPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__global int*) ({this}))")
    public GlobalIntPtr asIntPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__global long*) ({this}))")
    public GlobalLongPtr asLongPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__global double*) ({this}))")
    public GlobalDoublePtr asDoublePtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__global float*) ({this}))")
    public GlobalFloatPtr asFloatPtr() {
        return null;
    }
}
