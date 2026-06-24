package net.sixik.ga_utils.javatogpu.frontend;

import net.sixik.ga_utils.javatogpu.types.GpuTypeSupport;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Resolves {@code @GPUStruct} types through stable aliases.
 *
 * <p>The registry keeps unique aliases available and drops aliases that become ambiguous, so callers can still use
 * fully-qualified or owner-scoped names such as {@code Left.Point} even when multiple structs share the same leaf
 * name {@code Point}.
 */
public record GpuStructAliasRegistry<T>(Map<String, T> aliases, Set<String> ambiguousAliases) {

    public static <T> GpuStructAliasRegistry<T> empty() {
        return new GpuStructAliasRegistry<>(Map.of(), Set.of());
    }

    public static <T> GpuStructAliasRegistry<T> create(
            Collection<T> values,
            Function<T, String> ownerSimpleName,
            Function<T, String> ownerQualifiedName,
            BiPredicate<T, T> sameValue
    ) {
        Map<String, T> aliases = new LinkedHashMap<>();
        Set<String> ambiguousAliases = new LinkedHashSet<>();

        for (T value : values) {
            for (String alias : aliasesFor(ownerSimpleName.apply(value), ownerQualifiedName.apply(value))) {
                registerAlias(aliases, ambiguousAliases, alias, value, sameValue);
            }
        }

        return new GpuStructAliasRegistry<>(
                Collections.unmodifiableMap(new LinkedHashMap<>(aliases)),
                Collections.unmodifiableSet(new LinkedHashSet<>(ambiguousAliases))
        );
    }

    public T resolve(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return null;
        }
        if (ambiguousAliases.contains(typeName)) {
            return null;
        }

        T direct = aliases.get(typeName);
        if (direct != null) {
            return direct;
        }

        String simpleName = GpuTypeSupport.simpleTypeName(typeName);
        if (ambiguousAliases.contains(simpleName)) {
            return null;
        }
        return aliases.get(simpleName);
    }

    public boolean isAmbiguous(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            return false;
        }
        return ambiguousAliases.contains(typeName) || ambiguousAliases.contains(GpuTypeSupport.simpleTypeName(typeName));
    }

    private static <T> void registerAlias(
            Map<String, T> aliases,
            Set<String> ambiguousAliases,
            String alias,
            T value,
            BiPredicate<T, T> sameValue
    ) {
        if (alias == null || alias.isBlank() || ambiguousAliases.contains(alias)) {
            return;
        }

        T existing = aliases.putIfAbsent(alias, value);
        if (existing != null && !sameValue.test(existing, value)) {
            aliases.remove(alias);
            ambiguousAliases.add(alias);
        }
    }

    private static Set<String> aliasesFor(String ownerSimpleName, String ownerQualifiedName) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        addAlias(aliases, ownerQualifiedName);
        if (ownerQualifiedName != null && !ownerQualifiedName.isBlank()) {
            int separator = ownerQualifiedName.indexOf('.');
            while (separator >= 0) {
                addAlias(aliases, ownerQualifiedName.substring(separator + 1));
                separator = ownerQualifiedName.indexOf('.', separator + 1);
            }
        }
        addAlias(aliases, ownerSimpleName);
        addAlias(aliases, GpuTypeSupport.simpleTypeName(ownerSimpleName));
        return aliases;
    }

    private static void addAlias(Set<String> aliases, String alias) {
        if (alias != null && !alias.isBlank()) {
            aliases.add(alias);
        }
    }
}
