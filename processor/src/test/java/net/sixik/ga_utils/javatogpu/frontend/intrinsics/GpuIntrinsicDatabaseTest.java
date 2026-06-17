package net.sixik.ga_utils.javatogpu.frontend.intrinsics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpuIntrinsicDatabaseTest {

    @Test
    void resolvesMathAndBuiltinIntrinsics() {
        GpuIntrinsicDatabase database = GpuIntrinsicDatabase.createDefault();

        GpuIntrinsic sin = database.require("GPU", "sin", 1);
        GpuIntrinsic cos = database.require("GPU", "cos", 1);
        GpuIntrinsic globalId = database.require("GPU", "get_global_id", 1);

        assertEquals(GpuIntrinsicKind.MATH, sin.kind());
        assertEquals("sin", sin.backendName());
        assertEquals("float", sin.resultType());
        assertEquals(List.of("float"), sin.argumentTypes());
        assertEquals(GpuIntrinsicKind.MATH, cos.kind());
        assertEquals("cos", cos.backendName());
        assertEquals(GpuIntrinsicKind.BUILTIN_ID, globalId.kind());
        assertEquals("get_global_id", globalId.backendName());
        assertEquals("int", globalId.resultType());
        assertEquals(List.of("int"), globalId.argumentTypes());
        assertTrue(database.isAllowedOwner("GPU"));
        assertTrue(database.isAllowedAllocationType("IntPtr"));
    }
}
