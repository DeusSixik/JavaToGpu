package net.sixik.ga_utils.javatogpu.runtime.opencl;

import net.sixik.ga_utils.javatogpu.api.anotations.GPUStruct;
import net.sixik.ga_utils.javatogpu.api.anotations.OpenCLAttributes;
import net.sixik.ga_utils.javatogpu.types.GpuTypeSupport;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OpenClValuePacker {

    private static final Pattern ALIGNED_PATTERN = Pattern.compile("aligned\\((\\d+)\\)");
    private static final Map<Class<?>, StructLayout> STRUCT_LAYOUT_CACHE = new ConcurrentHashMap<>();

    private OpenClValuePacker() {
    }

    static boolean isStructInstance(Object value) {
        return value.getClass().isAnnotationPresent(GPUStruct.class);
    }

    static ByteBuffer packVector(String javaType, Object argument) {
        List<String> fieldNames = GpuTypeSupport.vectorFieldNames(javaType);
        String componentType = GpuTypeSupport.vectorComponentType(javaType, "x");
        ByteBuffer buffer = zeroedBuffer(GpuTypeSupport.vectorByteSize(javaType));
        int offset = 0;
        int componentSize = GpuTypeSupport.scalarByteSize(componentType);

        for (String fieldName : fieldNames) {
            writeScalar(buffer, offset, componentType, readFieldValue(argument, requireField(argument.getClass(), fieldName)));
            offset += componentSize;
        }
        for (int i = fieldNames.size(); i < GpuTypeSupport.vectorStorageWidth(javaType); i++) {
            writeZeroScalar(buffer, offset, componentType);
            offset += componentSize;
        }

        buffer.limit(buffer.capacity());
        buffer.position(0);
        return buffer;
    }

    static ByteBuffer packStruct(Object argument) {
        StructLayout layout = resolveStructLayout(argument.getClass());
        ByteBuffer buffer = zeroedBuffer(layout.size());
        layout.write(argument, buffer, 0);
        buffer.limit(layout.size());
        buffer.position(0);
        return buffer;
    }

    private static StructLayout createStructLayout(Class<?> type) {
        if (!type.isAnnotationPresent(GPUStruct.class)) {
            throw new IllegalArgumentException("Unsupported OpenCL struct argument type: " + type.getName());
        }

        boolean packed = hasPacked(type.getAnnotation(OpenCLAttributes.class));
        int structAlignment = 1;
        int offset = 0;
        List<FieldLayout> fields = new ArrayList<>();

        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }

            field.setAccessible(true);
            ValueLayout valueLayout = layoutFor(field.getType());
            int fieldAlignment = packed ? 1 : valueLayout.alignment();
            int explicitFieldAlignment = explicitAlignment(field.getAnnotation(OpenCLAttributes.class));
            if (explicitFieldAlignment > 0) {
                fieldAlignment = Math.max(fieldAlignment, explicitFieldAlignment);
            }

            offset = align(offset, fieldAlignment);
            fields.add(new FieldLayout(field, offset, valueLayout.size(), valueLayout));
            offset += valueLayout.size();
            structAlignment = Math.max(structAlignment, fieldAlignment);
        }

        int explicitStructAlignment = explicitAlignment(type.getAnnotation(OpenCLAttributes.class));
        if (explicitStructAlignment > 0) {
            structAlignment = Math.max(structAlignment, explicitStructAlignment);
        }

        return new StructLayout(align(offset, structAlignment), structAlignment, List.copyOf(fields));
    }

    private static ValueLayout layoutFor(Class<?> type) {
        if (type.isPrimitive()) {
            return scalarLayout(type.getName());
        }
        if (GpuTypeSupport.isSupportedVectorType(type.getName())) {
            return new VectorLayout(type.getName());
        }
        if (type.isAnnotationPresent(GPUStruct.class)) {
            return resolveStructLayout(type);
        }
        throw new IllegalArgumentException("Unsupported @GPUStruct field type for OpenCL marshalling: " + type.getName());
    }

    private static StructLayout resolveStructLayout(Class<?> type) {
        StructLayout cached = STRUCT_LAYOUT_CACHE.get(type);
        if (cached != null) {
            return cached;
        }

        StructLayout created = createStructLayout(type);
        StructLayout existing = STRUCT_LAYOUT_CACHE.putIfAbsent(type, created);
        return existing != null ? existing : created;
    }

    private static ScalarLayout scalarLayout(String javaType) {
        return new ScalarLayout(javaType, GpuTypeSupport.scalarByteSize(javaType));
    }

    private static boolean hasPacked(OpenCLAttributes attributes) {
        if (attributes == null) {
            return false;
        }
        for (String attribute : attributes.value()) {
            if ("packed".equals(attribute.strip())) {
                return true;
            }
        }
        return false;
    }

    private static int explicitAlignment(OpenCLAttributes attributes) {
        if (attributes == null) {
            return 0;
        }

        int alignment = 0;
        for (String attribute : attributes.value()) {
            Matcher matcher = ALIGNED_PATTERN.matcher(attribute.strip());
            if (matcher.matches()) {
                alignment = Math.max(alignment, Integer.parseInt(matcher.group(1)));
            }
        }
        return alignment;
    }

    private static int align(int value, int alignment) {
        if (alignment <= 1) {
            return value;
        }
        int remainder = value % alignment;
        return remainder == 0 ? value : value + (alignment - remainder);
    }

    private static ByteBuffer zeroedBuffer(int size) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
        for (int i = 0; i < size; i++) {
            buffer.put((byte) 0);
        }
        buffer.clear();
        return buffer;
    }

    private static Field requireField(Class<?> ownerType, String fieldName) {
        Class<?> current = ownerType;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new IllegalArgumentException("Failed to read vector field '" + fieldName + "' from " + ownerType.getName());
    }

    private static Object readFieldValue(Object owner, Field field) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalArgumentException("Failed to read field '" + field.getName() + "' from " + owner.getClass().getName(), exception);
        }
    }

    private static void writeScalar(ByteBuffer buffer, int offset, String scalarType, Object value) {
        switch (scalarType) {
            case "byte" -> buffer.put(offset, ((Number) value).byteValue());
            case "short" -> buffer.putShort(offset, ((Number) value).shortValue());
            case "int" -> buffer.putInt(offset, ((Number) value).intValue());
            case "long" -> buffer.putLong(offset, ((Number) value).longValue());
            case "float" -> buffer.putFloat(offset, ((Number) value).floatValue());
            case "double" -> buffer.putDouble(offset, ((Number) value).doubleValue());
            case "boolean" -> buffer.put(offset, Boolean.TRUE.equals(value) ? (byte) 1 : (byte) 0);
            default -> throw new IllegalArgumentException("Unsupported OpenCL scalar type for marshalling: " + scalarType);
        }
    }

    private static void writeZeroScalar(ByteBuffer buffer, int offset, String scalarType) {
        switch (scalarType) {
            case "byte", "boolean" -> buffer.put(offset, (byte) 0);
            case "short" -> buffer.putShort(offset, (short) 0);
            case "int" -> buffer.putInt(offset, 0);
            case "long" -> buffer.putLong(offset, 0L);
            case "float" -> buffer.putFloat(offset, 0.0f);
            case "double" -> buffer.putDouble(offset, 0.0d);
            default -> throw new IllegalArgumentException("Unsupported OpenCL scalar type for marshalling: " + scalarType);
        }
    }

    private interface ValueLayout {

        int size();

        int alignment();

        void write(Object value, ByteBuffer buffer, int offset);
    }

    private record ScalarLayout(String javaType, int size) implements ValueLayout {

        @Override
        public int alignment() {
            return size;
        }

        @Override
        public void write(Object value, ByteBuffer buffer, int offset) {
            writeScalar(buffer, offset, javaType, value);
        }
    }

    private static final class VectorLayout implements ValueLayout {

        private final String javaType;
        private final String componentType;
        private final List<String> fieldNames;
        private final int componentSize;
        private final int size;

        private VectorLayout(String javaType) {
            this.javaType = javaType;
            this.componentType = GpuTypeSupport.vectorComponentType(javaType, "x");
            this.fieldNames = GpuTypeSupport.vectorFieldNames(javaType);
            this.componentSize = GpuTypeSupport.scalarByteSize(componentType);
            this.size = GpuTypeSupport.vectorByteSize(javaType);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public int alignment() {
            return size;
        }

        @Override
        public void write(Object value, ByteBuffer buffer, int offset) {
            int cursor = offset;
            for (String fieldName : fieldNames) {
                writeScalar(buffer, cursor, componentType, readFieldValue(value, requireField(value.getClass(), fieldName)));
                cursor += componentSize;
            }
            for (int i = fieldNames.size(); i < GpuTypeSupport.vectorStorageWidth(javaType); i++) {
                writeZeroScalar(buffer, cursor, componentType);
                cursor += componentSize;
            }
        }
    }

    private record StructLayout(int size, int alignment, List<FieldLayout> fields) implements ValueLayout {

        @Override
        public void write(Object value, ByteBuffer buffer, int offset) {
            for (FieldLayout field : fields) {
                Object fieldValue = readFieldValue(value, field.field());
                if (fieldValue == null) {
                    throw new IllegalArgumentException("Null @GPUStruct field is not supported for OpenCL marshalling: " + field.field().getName());
                }
                field.layout().write(fieldValue, buffer, offset + field.offset());
            }
        }
    }

    private record FieldLayout(Field field, int offset, int storageSize, ValueLayout layout) {
    }
}
