package net.sixik.ga_utils.javatogpu.api;

import net.sixik.ga_utils.javatogpu.api.annotations.GPUPointerAddressSpace;
import net.sixik.ga_utils.javatogpu.api.annotations.GPUIntrinsic;
import net.sixik.ga_utils.javatogpu.api.annotations.GPUPointerType;

@GPUPointerType(valueType = "byte", addressSpace = GPUPointerAddressSpace.CONSTANT)
public final class ConstantBytePtr {

    public byte value;

    public ConstantBytePtr() {
    }

    public ConstantBytePtr(byte value) {
        this.value = value;
    }

    @GPUIntrinsic(code = "(({this}) + ({0}))")
    public ConstantBytePtr add(int bytes) {
        return this;
    }

    @GPUIntrinsic(code = "(({this}) - ({0}))")
    public ConstantBytePtr sub(int bytes) {
        return this;
    }

    @GPUIntrinsic(code = "((__constant char*) ({this}))")
    public ConstantCharPtr asCharPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__constant short*) ({this}))")
    public ConstantShortPtr asShortPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__constant int*) ({this}))")
    public ConstantIntPtr asIntPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__constant long*) ({this}))")
    public ConstantLongPtr asLongPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__constant float*) ({this}))")
    public ConstantFloatPtr asFloatPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__constant double*) ({this}))")
    public ConstantDoublePtr asDoublePtr() {
        return null;
    }
}
