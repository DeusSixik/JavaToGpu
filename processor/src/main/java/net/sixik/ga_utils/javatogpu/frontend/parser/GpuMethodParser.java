package net.sixik.ga_utils.javatogpu.frontend.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import net.sixik.ga_utils.javatogpu.frontend.model.GpuAddressSpace;
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuMethod;
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuParameter;

import java.util.List;

public final class GpuMethodParser {

    public ParsedGpuMethod parseMethod(String methodSource) {
        MethodDeclaration declaration = StaticJavaParser.parseMethodDeclaration(methodSource);

        List<ParsedGpuParameter> parameters = declaration.getParameters().stream()
                .map(this::toParsedParameter)
                .toList();

        return new ParsedGpuMethod(
                declaration.getNameAsString(),
                declaration.getTypeAsString(),
                parameters,
                declaration
        );
    }

    private ParsedGpuParameter toParsedParameter(Parameter parameter) {
        boolean isGlobal = parameter.getAnnotationByName("GPUGlobal").isPresent();
        boolean constant = parameter.getAnnotationByName("GPUGlobal")
                .filter(annotation -> annotation.isNormalAnnotationExpr())
                .flatMap(annotation -> annotation.asNormalAnnotationExpr().getPairs().stream()
                        .filter(pair -> pair.getNameAsString().equals("constant"))
                        .findFirst()
                        .map(pair -> pair.getValue().toString()))
                .map(Boolean::parseBoolean)
                .orElse(false);

        return new ParsedGpuParameter(
                parameter.getNameAsString(),
                parameter.getTypeAsString(),
                isGlobal ? GpuAddressSpace.GLOBAL : GpuAddressSpace.PRIVATE,
                constant
        );
    }
}
