package net.sixik.ga_utils.javatogpu.frontend.intrinsics;

import net.sixik.ga_utils.javatogpu.api.GPU;
import net.sixik.ga_utils.javatogpu.api.anotations.GPUIntrinsic;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GpuIntrinsicDatabase {

    private final Map<String, List<GpuIntrinsic>> intrinsics;
    private final List<GpuBuiltinConstant> builtinConstants;
    private final Set<String> allowedAllocationTypes;

    private GpuIntrinsicDatabase(
            Map<String, List<GpuIntrinsic>> intrinsics,
            List<GpuBuiltinConstant> builtinConstants,
            Set<String> allowedAllocationTypes
    ) {
        this.intrinsics = intrinsics;
        this.builtinConstants = builtinConstants;
        this.allowedAllocationTypes = allowedAllocationTypes;
    }

    public static GpuIntrinsicDatabase createDefault() {
        Map<String, List<GpuIntrinsic>> values = new HashMap<>();
        registerIntrinsicOwner(values, GPU.class);
        List<GpuBuiltinConstant> builtinConstants = readBuiltinConstants(GPU.class);

        return new GpuIntrinsicDatabase(
                values.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> List.copyOf(entry.getValue())
                )),
                List.copyOf(builtinConstants),
                Set.of("BytePtr", "CharPtr", "ShortPtr", "IntPtr", "LongPtr", "FloatPtr", "DoublePtr")
        );
    }

    public GpuIntrinsic require(String owner, String javaName, int arity) {
        List<GpuIntrinsic> matches = intrinsics.get(key(owner, javaName, arity));
        if (matches == null || matches.isEmpty()) {
            throw new IllegalArgumentException("Unknown GPU intrinsic: " + owner + "." + javaName + "/" + arity);
        }
        if (matches.size() != 1) {
            throw new IllegalArgumentException("Ambiguous GPU intrinsic overload: " + owner + "." + javaName + "/" + arity);
        }
        return matches.get(0);
    }

    public GpuIntrinsic require(String owner, String javaName, List<String> argumentTypes) {
        List<GpuIntrinsic> matches = intrinsics.get(key(owner, javaName, argumentTypes.size()));
        if (matches == null || matches.isEmpty()) {
            throw new IllegalArgumentException("Unknown GPU intrinsic: " + owner + "." + javaName + "/" + argumentTypes.size());
        }

        return matches.stream()
                .filter(intrinsic -> intrinsic.argumentTypes().equals(argumentTypes))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown GPU intrinsic overload: " + owner + "." + javaName + argumentTypes
                ));
    }

    public boolean isAllowedOwner(String owner) {
        return intrinsics.keySet().stream().anyMatch(key -> key.startsWith(owner + "#"));
    }

    public boolean isAllowedAllocationType(String typeName) {
        return allowedAllocationTypes.contains(typeName);
    }

    public List<GpuBuiltinConstant> builtinConstants() {
        return builtinConstants;
    }

    private static void register(Map<String, List<GpuIntrinsic>> values, GpuIntrinsic intrinsic) {
        values.computeIfAbsent(key(intrinsic.owner(), intrinsic.javaName(), intrinsic.arity()), ignored -> new ArrayList<>())
                .add(intrinsic);
    }

    private static void registerIntrinsicOwner(Map<String, List<GpuIntrinsic>> values, Class<?> ownerType) {
        for (Method method : ownerType.getDeclaredMethods()) {
            GPUIntrinsic annotation = method.getAnnotation(GPUIntrinsic.class);
            if (annotation == null) {
                continue;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalStateException("@GPUIntrinsic method must be static: "
                        + ownerType.getName() + "." + method.getName());
            }
            register(values, toIntrinsic(ownerType, method, annotation));
        }
    }

    private static GpuIntrinsic toIntrinsic(Class<?> ownerType, Method method, GPUIntrinsic annotation) {
        List<String> argumentTypes = javaTypeNames(method.getParameterTypes());
        String backendName = annotation.name().isBlank() ? method.getName() : annotation.name();
        return new GpuIntrinsic(
                ownerType.getSimpleName(),
                method.getName(),
                method.getParameterCount(),
                inferKind(backendName),
                backendName,
                javaTypeName(method.getReturnType()),
                argumentTypes
        );
    }

    private static GpuIntrinsicKind inferKind(String backendName) {
        if (backendName.startsWith("get_")) {
            return GpuIntrinsicKind.BUILTIN_ID;
        }
        if ("barrier".equals(backendName)) {
            return GpuIntrinsicKind.SYNCHRONIZATION;
        }
        if (Set.of("clamp", "mix", "step", "smoothstep", "hypot").contains(backendName)) {
            return GpuIntrinsicKind.COMMON;
        }
        return GpuIntrinsicKind.MATH;
    }

    private static List<GpuBuiltinConstant> readBuiltinConstants(Class<?> ownerType) {
        List<GpuBuiltinConstant> constants = new ArrayList<>();
        for (Field field : ownerType.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || !Modifier.isFinal(modifiers)) {
                continue;
            }
            String javaType = javaTypeName(field.getType());
            if (!isSupportedBuiltinConstantType(javaType)) {
                continue;
            }
            Object value;
            try {
                value = field.get(null);
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to read GPU builtin constant: "
                        + ownerType.getName() + "." + field.getName(), exception);
            }
            constants.add(new GpuBuiltinConstant(
                    ownerType.getSimpleName(),
                    ownerType.getName(),
                    field.getName(),
                    javaType,
                    builtinConstantSource(javaType, value)
            ));
        }
        return constants;
    }

    private static boolean isSupportedBuiltinConstantType(String javaType) {
        return switch (javaType) {
            case "byte", "short", "int", "long", "float", "double", "boolean", "char" -> true;
            default -> false;
        };
    }

    private static String builtinConstantSource(String javaType, Object value) {
        return switch (javaType) {
            case "boolean" -> String.valueOf(value);
            case "float" -> value + "f";
            case "double" -> String.valueOf(value);
            case "long" -> value + "L";
            case "char" -> Integer.toString((Character) value);
            default -> String.valueOf(value);
        };
    }

    private static List<String> javaTypeNames(Class<?>[] javaTypes) {
        List<String> names = new ArrayList<>(javaTypes.length);
        for (Class<?> javaType : javaTypes) {
            names.add(javaTypeName(javaType));
        }
        return List.copyOf(names);
    }

    private static String javaTypeName(Class<?> javaType) {
        if (javaType.isArray()) {
            return javaTypeName(javaType.getComponentType()) + "[]";
        }
        if (!javaType.isPrimitive()) {
            return javaType.getSimpleName();
        }
        if (javaType == Byte.TYPE) {
            return "byte";
        }
        if (javaType == Short.TYPE) {
            return "short";
        }
        if (javaType == Integer.TYPE) {
            return "int";
        }
        if (javaType == Long.TYPE) {
            return "long";
        }
        if (javaType == Float.TYPE) {
            return "float";
        }
        if (javaType == Double.TYPE) {
            return "double";
        }
        if (javaType == Boolean.TYPE) {
            return "boolean";
        }
        if (javaType == Character.TYPE) {
            return "char";
        }
        if (javaType == Void.TYPE) {
            return "void";
        }
        throw new IllegalArgumentException("Unsupported intrinsic Java type: " + javaType.getName());
    }

    private static String key(String owner, String javaName, int arity) {
        return owner + "#" + javaName + "#" + arity;
    }
}
