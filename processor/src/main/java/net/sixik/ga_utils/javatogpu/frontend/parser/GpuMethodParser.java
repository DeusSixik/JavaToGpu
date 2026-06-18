package net.sixik.ga_utils.javatogpu.frontend.parser;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.LiteralStringValueExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuConstant;
import net.sixik.ga_utils.javatogpu.frontend.model.GpuAddressSpace;
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuMethod;
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuParameter;

import java.util.List;

public final class GpuMethodParser {

    static {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
    }

    public ParsedGpuMethod parseMethod(String methodSource) {
        return parseMethod(methodSource, "", "", List.of());
    }

    public ParsedGpuMethod parseMethod(String methodSource, String ownerSimpleName, String ownerQualifiedName) {
        return parseMethod(methodSource, ownerSimpleName, ownerQualifiedName, List.of());
    }

    public ParsedGpuMethod parseMethod(
            String methodSource,
            String ownerSimpleName,
            String ownerQualifiedName,
            List<ParsedGpuConstant> constants
    ) {
        MethodDeclaration declaration = StaticJavaParser.parseMethodDeclaration(methodSource);

        List<ParsedGpuParameter> parameters = declaration.getParameters().stream()
                .map(this::toParsedParameter)
                .toList();

        return new ParsedGpuMethod(
                ownerSimpleName,
                ownerQualifiedName,
                declaration.getNameAsString(),
                declaration.getTypeAsString(),
                parameters,
                List.copyOf(constants),
                declaration,
                parseInlineFlag(declaration),
                parseNativeCode(declaration),
                declaration.isNative()
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

    private boolean parseInlineFlag(MethodDeclaration declaration) {
        return declaration.getAnnotationByName("CCode")
                .filter(annotation -> annotation.isNormalAnnotationExpr())
                .flatMap(annotation -> annotation.asNormalAnnotationExpr().getPairs().stream()
                        .filter(pair -> pair.getNameAsString().equals("inline"))
                        .findFirst()
                        .map(pair -> pair.getValue().toString()))
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    private String parseNativeCode(MethodDeclaration declaration) {
        return declaration.getAnnotationByName("CCode")
                .filter(annotation -> annotation.isNormalAnnotationExpr())
                .flatMap(annotation -> annotation.asNormalAnnotationExpr().getPairs().stream()
                        .filter(pair -> pair.getNameAsString().equals("code"))
                        .findFirst()
                        .map(pair -> pair.getValue()))
                .filter(LiteralStringValueExpr.class::isInstance)
                .map(LiteralStringValueExpr.class::cast)
                .map(this::literalStringValue)
                .orElse("");
    }

    private String literalStringValue(LiteralStringValueExpr literal) {
        if (literal instanceof TextBlockLiteralExpr textBlockLiteral) {
            return textBlockLiteral.asString();
        }
        if (literal instanceof StringLiteralExpr stringLiteral) {
            return stringLiteral.asString();
        }
        return literal.getValue();
    }
}
