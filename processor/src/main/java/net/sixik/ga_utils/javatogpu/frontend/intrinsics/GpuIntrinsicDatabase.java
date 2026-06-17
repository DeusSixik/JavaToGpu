package net.sixik.ga_utils.javatogpu.frontend.intrinsics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GpuIntrinsicDatabase {

    private final Map<String, GpuIntrinsic> intrinsics;
    private final Set<String> allowedAllocationTypes;

    private GpuIntrinsicDatabase(Map<String, GpuIntrinsic> intrinsics, Set<String> allowedAllocationTypes) {
        this.intrinsics = intrinsics;
        this.allowedAllocationTypes = allowedAllocationTypes;
    }

    public static GpuIntrinsicDatabase createDefault() {
        Map<String, GpuIntrinsic> values = new HashMap<>();
        register(values, new GpuIntrinsic("GPU", "sin", 1, GpuIntrinsicKind.MATH, "sin", "float", List.of("float")));
        register(values, new GpuIntrinsic("GPU", "cos", 1, GpuIntrinsicKind.MATH, "cos", "float", List.of("float")));
        register(values, new GpuIntrinsic("GPU", "sqrt", 1, GpuIntrinsicKind.MATH, "sqrt", "float", List.of("float")));
        register(values, new GpuIntrinsic("GPU", "fabs", 1, GpuIntrinsicKind.MATH, "fabs", "float", List.of("float")));
        register(values, new GpuIntrinsic("GPU", "get_global_id", 1, GpuIntrinsicKind.BUILTIN_ID, "get_global_id", "int", List.of("int")));

        return new GpuIntrinsicDatabase(
                Map.copyOf(values),
                Set.of("BytePtr", "CharPtr", "ShortPtr", "IntPtr", "LongPtr", "FloatPtr", "DoublePtr")
        );
    }

    public GpuIntrinsic require(String owner, String javaName, int arity) {
        GpuIntrinsic intrinsic = intrinsics.get(key(owner, javaName, arity));
        if (intrinsic == null) {
            throw new IllegalArgumentException("Unknown GPU intrinsic: " + owner + "." + javaName + "/" + arity);
        }
        return intrinsic;
    }

    public boolean isAllowedOwner(String owner) {
        return intrinsics.keySet().stream().anyMatch(key -> key.startsWith(owner + "#"));
    }

    public boolean isAllowedAllocationType(String typeName) {
        return allowedAllocationTypes.contains(typeName);
    }

    private static void register(Map<String, GpuIntrinsic> values, GpuIntrinsic intrinsic) {
        values.put(key(intrinsic.owner(), intrinsic.javaName(), intrinsic.arity()), intrinsic);
    }

    private static String key(String owner, String javaName, int arity) {
        return owner + "#" + javaName + "#" + arity;
    }
}
