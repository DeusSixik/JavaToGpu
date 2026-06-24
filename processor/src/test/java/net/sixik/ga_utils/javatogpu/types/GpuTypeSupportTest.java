package net.sixik.ga_utils.javatogpu.types;

import net.sixik.ga_utils.javatogpu.frontend.intrinsics.GpuIntrinsicDatabase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpuTypeSupportTest {

    @Test
    void discoversAnnotatedPointerTypesBySimpleAndQualifiedName() {
        assertTrue(GpuTypeSupport.isSupportedPointerType("IntPtr"));
        assertTrue(GpuTypeSupport.isSupportedPointerType("net.sixik.ga_utils.javatogpu.api.FloatPtr"));
        assertTrue(GpuTypeSupport.isSupportedPointerClassName("net.sixik.ga_utils.javatogpu.api.DoublePtr"));

        assertEquals("int", GpuTypeSupport.pointerValueType("IntPtr"));
        assertEquals("float", GpuTypeSupport.pointerValueType("net.sixik.ga_utils.javatogpu.api.FloatPtr"));
    }

    @Test
    void discoversAnnotatedScalarAliasTypesBySimpleAndQualifiedName() {
        assertTrue(GpuTypeSupport.isSupportedScalarAliasType("UInt"));
        assertTrue(GpuTypeSupport.isSupportedScalarAliasType("net.sixik.ga_utils.javatogpu.api.ULong"));
        assertTrue(GpuTypeSupport.isSupportedScalarAliasClassName("net.sixik.ga_utils.javatogpu.api.UShort"));

        assertEquals("uint", GpuTypeSupport.openClScalarAliasTypeName("UInt"));
        assertEquals("int", GpuTypeSupport.scalarAliasValueType("UInt"));
        assertEquals(Integer.BYTES, GpuTypeSupport.scalarByteSize("UInt"));
    }

    @Test
    void intrinsicAllocationWhitelistUsesAnnotatedPointerAndAliasTypes() {
        GpuIntrinsicDatabase database = GpuIntrinsicDatabase.createDefault();

        assertTrue(database.isAllowedAllocationType("BytePtr"));
        assertTrue(database.isAllowedAllocationType("net.sixik.ga_utils.javatogpu.api.UInt"));
    }
}
