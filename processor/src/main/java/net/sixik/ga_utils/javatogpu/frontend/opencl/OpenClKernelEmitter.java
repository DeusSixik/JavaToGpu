package net.sixik.ga_utils.javatogpu.frontend.opencl;
import net.sixik.ga_utils.javatogpu.frontend.ir.expression.GpuIrArrayAccess;
import net.sixik.ga_utils.javatogpu.frontend.ir.expression.GpuIrBinary;
import net.sixik.ga_utils.javatogpu.frontend.ir.expression.GpuIrExpression;
import net.sixik.ga_utils.javatogpu.frontend.ir.expression.GpuIrIntrinsicCall;
import net.sixik.ga_utils.javatogpu.frontend.ir.expression.GpuIrLiteral;
import net.sixik.ga_utils.javatogpu.frontend.ir.expression.GpuIrTernary;
import net.sixik.ga_utils.javatogpu.frontend.ir.expression.GpuIrUnary;
import net.sixik.ga_utils.javatogpu.frontend.ir.expression.GpuIrVariableRef;
import net.sixik.ga_utils.javatogpu.frontend.ir.model.GpuIrMethod;
import net.sixik.ga_utils.javatogpu.frontend.ir.statement.GpuIrAssignment;
import net.sixik.ga_utils.javatogpu.frontend.ir.statement.GpuIrForLoop;
import net.sixik.ga_utils.javatogpu.frontend.ir.statement.GpuIrIf;
import net.sixik.ga_utils.javatogpu.frontend.ir.statement.GpuIrStatement;
import net.sixik.ga_utils.javatogpu.frontend.ir.statement.GpuIrVariableDeclaration;
import net.sixik.ga_utils.javatogpu.frontend.model.GpuAddressSpace;
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuMethod;
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuParameter;

import java.util.stream.Collectors;

public final class OpenClKernelEmitter {

    public String emit(ParsedGpuMethod parsedMethod, GpuIrMethod irMethod) {
        String entryPointName = OpenClKernelNaming.toEntryPointName(irMethod.name());
        StringBuilder builder = new StringBuilder();
        builder.append("__kernel void ")
                .append(entryPointName)
                .append("(")
                .append(parsedMethod.parameters().stream().map(this::emitParameter).collect(Collectors.joining(", ")))
                .append(") {\n");

        for (GpuIrStatement statement : irMethod.statements()) {
            emitStatement(builder, statement, 1);
        }

        builder.append("}");
        return builder.toString();
    }

    private String emitParameter(ParsedGpuParameter parameter) {
        String type = parameter.javaType();
        if (type.endsWith("[]")) {
            String elementType = type.substring(0, type.length() - 2);
            if (parameter.addressSpace() == GpuAddressSpace.GLOBAL) {
                return "__global " + (parameter.constant() ? "const " : "") + elementType + "* " + parameter.name();
            }
            return elementType + "* " + parameter.name();
        }

        if (parameter.addressSpace() == GpuAddressSpace.GLOBAL) {
            return "__global " + type + "* " + parameter.name();
        }
        return type + " " + parameter.name();
    }

    private void emitStatement(StringBuilder builder, GpuIrStatement statement, int indent) {
        String prefix = "    ".repeat(indent);

        if (statement instanceof GpuIrVariableDeclaration declaration) {
            builder.append(prefix)
                    .append(declaration.typeName())
                    .append(" ")
                    .append(declaration.name())
                    .append(" = ")
                    .append(emitExpression(declaration.initializer()))
                    .append(";\n");
            return;
        }

        if (statement instanceof GpuIrAssignment assignment) {
            builder.append(prefix)
                    .append(emitExpression(assignment.target()))
                    .append(" = ")
                    .append(emitExpression(assignment.value()))
                    .append(";\n");
            return;
        }

        if (statement instanceof GpuIrForLoop loop) {
            builder.append(prefix)
                    .append("for (")
                    .append(emitForHeaderStatement(loop.initializer()))
                    .append("; ")
                    .append(emitExpression(loop.condition()))
                    .append("; ")
                    .append(emitForHeaderStatement(loop.update()))
                    .append(") {\n");

            for (GpuIrStatement bodyStatement : loop.body()) {
                emitStatement(builder, bodyStatement, indent + 1);
            }

            builder.append(prefix).append("}\n");
            return;
        }

        if (statement instanceof GpuIrIf ifStatement) {
            builder.append(prefix)
                    .append("if (")
                    .append(emitExpression(ifStatement.condition()))
                    .append(") {\n");

            for (GpuIrStatement thenStatement : ifStatement.thenBranch()) {
                emitStatement(builder, thenStatement, indent + 1);
            }

            if (!ifStatement.elseBranch().isEmpty()) {
                builder.append(prefix).append("} else {\n");
                for (GpuIrStatement elseStatement : ifStatement.elseBranch()) {
                    emitStatement(builder, elseStatement, indent + 1);
                }
            }

            builder.append(prefix).append("}\n");
            return;
        }

        throw new IllegalArgumentException("Unsupported IR statement: " + statement);
    }

    private String emitForHeaderStatement(GpuIrStatement statement) {
        if (statement instanceof GpuIrVariableDeclaration declaration) {
            return declaration.typeName() + " " + declaration.name() + " = " + emitExpression(declaration.initializer());
        }
        if (statement instanceof GpuIrAssignment assignment) {
            return emitExpression(assignment.target()) + " = " + emitExpression(assignment.value());
        }
        throw new IllegalArgumentException("Unsupported for-header statement: " + statement);
    }

    private String emitExpression(GpuIrExpression expression) {
        if (expression instanceof GpuIrVariableRef variableRef) {
            return variableRef.name();
        }

        if (expression instanceof GpuIrLiteral literal) {
            return literal.sourceText();
        }

        if (expression instanceof GpuIrArrayAccess arrayAccess) {
            return arrayAccess.arrayName() + "[" + emitExpression(arrayAccess.index()) + "]";
        }

        if (expression instanceof GpuIrIntrinsicCall intrinsicCall) {
            return intrinsicCall.backendName()
                    + "("
                    + intrinsicCall.arguments().stream().map(this::emitExpression).collect(Collectors.joining(", "))
                    + ")";
        }

        if (expression instanceof GpuIrBinary binary) {
            return "(" + emitExpression(binary.left()) + " " + binary.operator() + " " + emitExpression(binary.right()) + ")";
        }

        if (expression instanceof GpuIrUnary unary) {
            return "(" + unary.operator() + emitExpression(unary.operand()) + ")";
        }

        if (expression instanceof GpuIrTernary ternary) {
            return "("
                    + emitExpression(ternary.condition())
                    + " ? "
                    + emitExpression(ternary.whenTrue())
                    + " : "
                    + emitExpression(ternary.whenFalse())
                    + ")";
        }

        throw new IllegalArgumentException("Unsupported IR expression: " + expression);
    }
}
