package net.sixik.ga_utils.javatogpu.api;

import net.sixik.ga_utils.javatogpu.api.annotations.GPUPointerAddressSpace;
import net.sixik.ga_utils.javatogpu.api.annotations.GPUIntrinsic;
import net.sixik.ga_utils.javatogpu.api.annotations.GPUPointerType;

@GPUPointerType(valueType = "byte", addressSpace = GPUPointerAddressSpace.LOCAL)
public final class LocalBytePtr {

    public byte value;

    public LocalBytePtr() {
    }

    public LocalBytePtr(byte value) {
        this.value = value;
    }

    @GPUIntrinsic(code = "(({this}) + ({0}))")
    public LocalBytePtr add(int bytes) {
        return this;
    }

    @GPUIntrinsic(code = "(({this}) - ({0}))")
    public LocalBytePtr sub(int bytes) {
        return this;
    }

    @GPUIntrinsic(code = "((__local char*) ({this}))")
    public LocalCharPtr asCharPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__local short*) ({this}))")
    public LocalShortPtr asShortPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__local int*) ({this}))")
    public LocalIntPtr asIntPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__local long*) ({this}))")
    public LocalLongPtr asLongPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__local float*) ({this}))")
    public LocalFloatPtr asFloatPtr() {
        return null;
    }

    @GPUIntrinsic(code = "((__local double*) ({this}))")
    public LocalDoublePtr asDoublePtr() {
        return null;
    }
}
