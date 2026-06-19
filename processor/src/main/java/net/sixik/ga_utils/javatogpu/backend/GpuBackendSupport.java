package net.sixik.ga_utils.javatogpu.backend;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import net.sixik.ga_utils.javatogpu.api.GpuBackendTarget;
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuMethod;

import java.util.EnumSet;
import java.util.Properties;

public final class GpuBackendSupport {

    private GpuBackendSupport() {
    }

    public static boolean supportsBackend(GpuBackendTarget[] backends, GpuBackendTarget backendTarget) {
        for (GpuBackendTarget backend : backends) {
            if (backend == backendTarget) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsBackend(Properties properties, String keyPrefix, GpuBackendTarget backendTarget) {
        int count = Integer.parseInt(properties.getProperty(keyPrefix + ".count", "0"));
        if (count == 0) {
            return true;
        }
        for (int i = 0; i < count; i++) {
            if (backendTarget.name().equals(properties.getProperty(keyPrefix + "." + i))) {
                return true;
            }
        }
        return false;
    }

    public static void storeBackends(Properties properties, String keyPrefix, GpuBackendTarget[] backends) {
        properties.setProperty(keyPrefix + ".count", Integer.toString(backends.length));
        for (int i = 0; i < backends.length; i++) {
            properties.setProperty(keyPrefix + "." + i, backends[i].name());
        }
    }

    public static boolean supportsParsedMethodBackend(
            ParsedGpuMethod method,
            String annotationName,
            GpuBackendTarget backendTarget
    ) {
        return method.declaration().getAnnotationByName(annotationName)
                .map(annotation -> supportsAnnotationBackend(annotation, backendTarget))
                .orElse(false);
    }

    public static boolean supportsAnnotationBackend(AnnotationExpr annotation, GpuBackendTarget backendTarget) {
        if (!annotation.isNormalAnnotationExpr()) {
            return true;
        }
        return annotation.asNormalAnnotationExpr().getPairs().stream()
                .filter(pair -> pair.getNameAsString().equals("backends"))
                .findFirst()
                .map(pair -> parseBackendTargets(pair.getValue()).contains(backendTarget))
                .orElse(true);
    }

    public static EnumSet<GpuBackendTarget> parseBackendTargets(Expression expression) {
        EnumSet<GpuBackendTarget> backends = EnumSet.noneOf(GpuBackendTarget.class);
        if (expression instanceof ArrayInitializerExpr arrayInitializerExpr) {
            for (Expression value : arrayInitializerExpr.getValues()) {
                backends.add(parseBackendTarget(value));
            }
        } else {
            backends.add(parseBackendTarget(expression));
        }
        return backends.isEmpty() ? EnumSet.of(GpuBackendTarget.OPENCL) : backends;
    }

    public static GpuBackendTarget parseBackendTarget(Expression expression) {
        String name;
        if (expression instanceof FieldAccessExpr fieldAccessExpr) {
            name = fieldAccessExpr.getNameAsString();
        } else if (expression instanceof NameExpr nameExpr) {
            name = nameExpr.getNameAsString();
        } else {
            throw new IllegalStateException("Unsupported backend target expression: " + expression);
        }
        return GpuBackendTarget.valueOf(name);
    }
}
