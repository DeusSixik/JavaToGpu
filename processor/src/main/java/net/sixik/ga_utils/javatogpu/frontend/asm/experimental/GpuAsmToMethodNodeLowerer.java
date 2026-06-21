package net.sixik.ga_utils.javatogpu.frontend.asm.experimental;

import net.sixik.ga_utils.javatogpu.frontend.asm.GpuFriendlyAsmMethodBuilder;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Optional lowering helper from a tiny neutral AST into canonical GPU-friendly ASM.
 *
 * <p>This is intentionally small and conservative. It is aimed at generator authors and examples,
 * not at regular end users.</p>
 */
public final class GpuAsmToMethodNodeLowerer {

    public MethodNode lowerStaticMethod(GpuAsmMethod method) {
        Objects.requireNonNull(method, "method");
        String descriptor = Type.getMethodDescriptor(
                method.returnType(),
                method.parameters().stream().map(GpuAsmParameter::type).toArray(Type[]::new)
        );
        GpuFriendlyAsmMethodBuilder builder = GpuFriendlyAsmMethodBuilder.staticMethod(method.name(), descriptor);

        LoweringContext context = new LoweringContext(builder);
        for (int index = 0; index < method.parameters().size(); index++) {
            GpuAsmParameter parameter = method.parameters().get(index);
            context.localsByName.put(parameter.name(), new LocalBinding(builder.parameterSlot(index), parameter.type()));
        }

        lowerStatements(method.statements(), context);
        return builder.toMethodNode();
    }

    private void lowerStatements(List<GpuAsmStmt> statements, LoweringContext context) {
        for (GpuAsmStmt statement : statements) {
            lowerStatement(statement, context);
        }
    }

    private void lowerStatement(GpuAsmStmt statement, LoweringContext context) {
        if (statement instanceof GpuAsmStmt.LocalVar) {
            GpuAsmStmt.LocalVar localVar = (GpuAsmStmt.LocalVar) statement;
            emitExpression(localVar.initializer(), context);
            int slot = context.builder.newTemp(localVar.type());
            context.localsByName.put(localVar.name(), new LocalBinding(slot, localVar.type()));
            context.builder.storeLocal(slot, localVar.type());
            return;
        }
        if (statement instanceof GpuAsmStmt.AssignLocal) {
            GpuAsmStmt.AssignLocal assignLocal = (GpuAsmStmt.AssignLocal) statement;
            LocalBinding binding = requireLocal(context, assignLocal.name());
            emitExpression(assignLocal.value(), context);
            context.builder.storeLocal(binding.slot(), binding.type());
            return;
        }
        if (statement instanceof GpuAsmStmt.ArrayStore) {
            GpuAsmStmt.ArrayStore arrayStore = (GpuAsmStmt.ArrayStore) statement;
            LocalBinding arrayBinding = requireLocal(context, arrayStore.arrayName());
            context.builder.loadLocal(arrayBinding.slot(), arrayBinding.type());
            emitExpression(arrayStore.index(), context);
            emitExpression(arrayStore.value(), context);
            context.builder.emitArrayStore(arrayStore.elementType());
            return;
        }
        if (statement instanceof GpuAsmStmt.IfElse) {
            GpuAsmStmt.IfElse ifElse = (GpuAsmStmt.IfElse) statement;
            if (ifElse.elseBranch().isEmpty()) {
                context.builder.emitCanonicalIf(
                        builder -> emitCondition(ifElse.condition(), context),
                        falseJumpOpcode(ifElse.condition(), context),
                        builder -> lowerStatements(ifElse.thenBranch(), context)
                );
            } else {
                context.builder.emitCanonicalIfElse(
                        builder -> emitCondition(ifElse.condition(), context),
                        falseJumpOpcode(ifElse.condition(), context),
                        builder -> lowerStatements(ifElse.thenBranch(), context),
                        builder -> lowerStatements(ifElse.elseBranch(), context)
                );
            }
            return;
        }
        if (statement instanceof GpuAsmStmt.WhileLoop) {
            GpuAsmStmt.WhileLoop whileLoop = (GpuAsmStmt.WhileLoop) statement;
            context.builder.emitCanonicalWhileLoop(
                    builder -> emitCondition(whileLoop.condition(), context),
                    falseJumpOpcode(whileLoop.condition(), context),
                    builder -> lowerStatements(whileLoop.body(), context)
            );
            return;
        }
        if (statement instanceof GpuAsmStmt.DoWhileLoop) {
            GpuAsmStmt.DoWhileLoop doWhileLoop = (GpuAsmStmt.DoWhileLoop) statement;
            context.builder.emitCanonicalDoWhileLoop(
                    builder -> lowerStatements(doWhileLoop.body(), context),
                    builder -> emitCondition(doWhileLoop.condition(), context),
                    trueJumpOpcode(doWhileLoop.condition(), context)
            );
            return;
        }
        if (statement instanceof GpuAsmStmt.SwitchStmt) {
            GpuAsmStmt.SwitchStmt switchStmt = (GpuAsmStmt.SwitchStmt) statement;
            context.builder.emitCanonicalSwitch(
                    builder -> emitExpression(switchStmt.selector(), context),
                    switchStmt.cases().stream()
                            .map(switchCase -> GpuFriendlyAsmMethodBuilder.SwitchCase.of(
                                    switchCase.keys(),
                                    builder -> lowerStatements(switchCase.statements(), context),
                                    switchCase.fallThrough()
                            ))
                            .toList(),
                    builder -> lowerStatements(switchStmt.defaultStatements(), context)
            );
            return;
        }
        if (statement instanceof GpuAsmStmt.ExprStmt) {
            GpuAsmStmt.ExprStmt exprStmt = (GpuAsmStmt.ExprStmt) statement;
            emitExpression(exprStmt.expression(), context);
            return;
        }
        if (statement instanceof GpuAsmStmt.BreakSwitch) {
            context.builder.emitBreakSwitch();
            return;
        }
        if (statement instanceof GpuAsmStmt.BreakLoop) {
            context.builder.emitBreakLoop();
            return;
        }
        if (statement instanceof GpuAsmStmt.ContinueLoop) {
            context.builder.emitContinueLoop();
            return;
        }
        if (statement instanceof GpuAsmStmt.ReturnValue) {
            GpuAsmStmt.ReturnValue returnValue = (GpuAsmStmt.ReturnValue) statement;
            emitExpression(returnValue.value(), context);
            context.builder.emitReturn(expressionType(returnValue.value(), context));
            return;
        }
        if (statement instanceof GpuAsmStmt.ReturnVoid) {
            context.builder.emitVoidReturn();
            return;
        }
        throw new IllegalArgumentException("Unsupported experimental GPU ASM statement: " + statement.getClass().getName());
    }

    private void emitExpression(GpuAsmExpr expression, LoweringContext context) {
        if (expression instanceof GpuAsmExpr.IntLiteral) {
            GpuAsmExpr.IntLiteral literal = (GpuAsmExpr.IntLiteral) expression;
            context.builder.pushInt(literal.value());
            return;
        }
        if (expression instanceof GpuAsmExpr.LongLiteral) {
            GpuAsmExpr.LongLiteral literal = (GpuAsmExpr.LongLiteral) expression;
            context.builder.pushLong(literal.value());
            return;
        }
        if (expression instanceof GpuAsmExpr.FloatLiteral) {
            GpuAsmExpr.FloatLiteral literal = (GpuAsmExpr.FloatLiteral) expression;
            context.builder.pushFloat(literal.value());
            return;
        }
        if (expression instanceof GpuAsmExpr.DoubleLiteral) {
            GpuAsmExpr.DoubleLiteral literal = (GpuAsmExpr.DoubleLiteral) expression;
            context.builder.pushDouble(literal.value());
            return;
        }
        if (expression instanceof GpuAsmExpr.LocalRef) {
            GpuAsmExpr.LocalRef localRef = (GpuAsmExpr.LocalRef) expression;
            LocalBinding binding = requireLocal(context, localRef.name());
            context.builder.loadLocal(binding.slot(), binding.type());
            return;
        }
        if (expression instanceof GpuAsmExpr.ArrayLoad) {
            GpuAsmExpr.ArrayLoad arrayLoad = (GpuAsmExpr.ArrayLoad) expression;
            LocalBinding arrayBinding = requireLocal(context, arrayLoad.arrayName());
            context.builder.loadLocal(arrayBinding.slot(), arrayBinding.type());
            emitExpression(arrayLoad.index(), context);
            context.builder.emitArrayLoad(arrayLoad.elementType());
            return;
        }
        if (expression instanceof GpuAsmExpr.Binary) {
            GpuAsmExpr.Binary binary = (GpuAsmExpr.Binary) expression;
            emitExpression(binary.left(), context);
            emitExpression(binary.right(), context);
            context.builder.emitInsn(binaryOpcode(binary.operator(), binary.resultType()));
            return;
        }
        if (expression instanceof GpuAsmExpr.Unary) {
            emitUnary((GpuAsmExpr.Unary) expression, context);
            return;
        }
        if (expression instanceof GpuAsmExpr.Cast) {
            GpuAsmExpr.Cast cast = (GpuAsmExpr.Cast) expression;
            emitExpression(cast.expression(), context);
            int opcode = castOpcode(expressionType(cast.expression(), context), cast.targetType());
            if (opcode != Opcodes.NOP) {
                context.builder.emitInsn(opcode);
            }
            return;
        }
        if (expression instanceof GpuAsmExpr.StaticCall) {
            GpuAsmExpr.StaticCall staticCall = (GpuAsmExpr.StaticCall) expression;
            for (GpuAsmExpr argument : staticCall.arguments()) {
                emitExpression(argument, context);
            }
            context.builder.emitStaticCall(staticCall.ownerInternalName(), staticCall.methodName(), staticCall.descriptor());
            return;
        }
        throw new IllegalArgumentException("Unsupported experimental GPU ASM expression: " + expression.getClass().getName());
    }

    private void emitCondition(GpuAsmCondition condition, LoweringContext context) {
        if (condition instanceof GpuAsmCondition.Comparison) {
            GpuAsmCondition.Comparison comparison = (GpuAsmCondition.Comparison) condition;
            emitExpression(comparison.left(), context);
            emitExpression(comparison.right(), context);
            Type comparisonType = comparisonType(comparison, context);
            if (comparisonType.getSort() == Type.LONG) {
                context.builder.emitInsn(Opcodes.LCMP);
            } else if (comparisonType.getSort() == Type.FLOAT) {
                context.builder.emitInsn(floatCompareOpcode(comparison.operator()));
            } else if (comparisonType.getSort() == Type.DOUBLE) {
                context.builder.emitInsn(doubleCompareOpcode(comparison.operator()));
            }
            return;
        }
        if (condition instanceof GpuAsmCondition.Truthy) {
            GpuAsmCondition.Truthy truthy = (GpuAsmCondition.Truthy) condition;
            emitExpression(truthy.expression(), context);
            Type truthyType = expressionType(truthy.expression(), context);
            if (truthyType.getSort() == Type.LONG) {
                context.builder.pushLong(0L);
                context.builder.emitInsn(Opcodes.LCMP);
            } else if (truthyType.getSort() == Type.FLOAT) {
                context.builder.pushFloat(0.0f);
                context.builder.emitInsn(Opcodes.FCMPG);
            } else if (truthyType.getSort() == Type.DOUBLE) {
                context.builder.pushDouble(0.0d);
                context.builder.emitInsn(Opcodes.DCMPG);
            }
            return;
        }
        throw new IllegalArgumentException("Unsupported experimental GPU ASM condition: " + condition.getClass().getName());
    }

    private int falseJumpOpcode(GpuAsmCondition condition, LoweringContext context) {
        if (condition instanceof GpuAsmCondition.Truthy) {
            return Opcodes.IFEQ;
        }
        if (condition instanceof GpuAsmCondition.Comparison) {
            GpuAsmCondition.Comparison comparison = (GpuAsmCondition.Comparison) condition;
            return comparisonFalseJumpOpcode(comparison.operator(), comparisonType(comparison, context));
        }
        throw new IllegalArgumentException("Unsupported experimental GPU ASM condition: " + condition.getClass().getName());
    }

    private int trueJumpOpcode(GpuAsmCondition condition, LoweringContext context) {
        if (condition instanceof GpuAsmCondition.Truthy) {
            return Opcodes.IFNE;
        }
        if (condition instanceof GpuAsmCondition.Comparison) {
            GpuAsmCondition.Comparison comparison = (GpuAsmCondition.Comparison) condition;
            return comparisonTrueJumpOpcode(comparison.operator(), comparisonType(comparison, context));
        }
        throw new IllegalArgumentException("Unsupported experimental GPU ASM condition: " + condition.getClass().getName());
    }

    private int binaryOpcode(String operator, Type resultType) {
        return switch (operator) {
            case "+" -> resultType.getOpcode(Opcodes.IADD);
            case "-" -> resultType.getOpcode(Opcodes.ISUB);
            case "*" -> resultType.getOpcode(Opcodes.IMUL);
            case "/" -> resultType.getOpcode(Opcodes.IDIV);
            case "%" -> resultType.getOpcode(Opcodes.IREM);
            case "&" -> resultType.getOpcode(Opcodes.IAND);
            case "|" -> resultType.getOpcode(Opcodes.IOR);
            case "^" -> resultType.getOpcode(Opcodes.IXOR);
            case "<<" -> resultType.getOpcode(Opcodes.ISHL);
            case ">>" -> resultType.getOpcode(Opcodes.ISHR);
            case ">>>" -> resultType.getOpcode(Opcodes.IUSHR);
            default -> throw new IllegalArgumentException("Unsupported binary operator: " + operator);
        };
    }

    private void emitUnary(GpuAsmExpr.Unary unary, LoweringContext context) {
        emitExpression(unary.expression(), context);
        Type operandType = expressionType(unary.expression(), context);
        switch (unary.operator()) {
            case "+" -> {
                return;
            }
            case "-" -> context.builder.emitInsn(operandType.getOpcode(Opcodes.INEG));
            case "~" -> emitBitwiseNot(operandType, context);
            default -> throw new IllegalArgumentException("Unsupported unary operator: " + unary.operator());
        }
    }

    private void emitBitwiseNot(Type operandType, LoweringContext context) {
        if (isIntLike(operandType)) {
            context.builder.pushInt(-1);
            context.builder.emitInsn(Opcodes.IXOR);
            return;
        }
        if (operandType.getSort() == Type.LONG) {
            context.builder.pushLong(-1L);
            context.builder.emitInsn(Opcodes.LXOR);
            return;
        }
        throw new IllegalArgumentException("Bitwise not is only supported for integral experimental GPU ASM values: " + operandType);
    }

    private int castOpcode(Type sourceType, Type targetType) {
        if (sourceType.equals(targetType)) {
            return Opcodes.NOP;
        }
        return switch (sourceType.getSort()) {
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> switch (targetType.getSort()) {
                case Type.BOOLEAN, Type.INT -> Opcodes.NOP;
                case Type.LONG -> Opcodes.I2L;
                case Type.FLOAT -> Opcodes.I2F;
                case Type.DOUBLE -> Opcodes.I2D;
                case Type.BYTE -> Opcodes.I2B;
                case Type.CHAR -> Opcodes.I2C;
                case Type.SHORT -> Opcodes.I2S;
                default -> throw new IllegalArgumentException("Unsupported cast in experimental GPU ASM AST: " + sourceType + " -> " + targetType);
            };
            case Type.LONG -> switch (targetType.getSort()) {
                case Type.INT -> Opcodes.L2I;
                case Type.FLOAT -> Opcodes.L2F;
                case Type.DOUBLE -> Opcodes.L2D;
                default -> throw new IllegalArgumentException("Unsupported cast in experimental GPU ASM AST: " + sourceType + " -> " + targetType);
            };
            case Type.FLOAT -> switch (targetType.getSort()) {
                case Type.INT -> Opcodes.F2I;
                case Type.LONG -> Opcodes.F2L;
                case Type.DOUBLE -> Opcodes.F2D;
                default -> throw new IllegalArgumentException("Unsupported cast in experimental GPU ASM AST: " + sourceType + " -> " + targetType);
            };
            case Type.DOUBLE -> switch (targetType.getSort()) {
                case Type.INT -> Opcodes.D2I;
                case Type.LONG -> Opcodes.D2L;
                case Type.FLOAT -> Opcodes.D2F;
                default -> throw new IllegalArgumentException("Unsupported cast in experimental GPU ASM AST: " + sourceType + " -> " + targetType);
            };
            default -> throw new IllegalArgumentException("Unsupported cast in experimental GPU ASM AST: " + sourceType + " -> " + targetType);
        };
    }

    private int comparisonFalseJumpOpcode(String operator, Type comparisonType) {
        if (isIntLike(comparisonType)) {
            return switch (operator) {
                case "==" -> Opcodes.IF_ICMPNE;
                case "!=" -> Opcodes.IF_ICMPEQ;
                case "<" -> Opcodes.IF_ICMPGE;
                case "<=" -> Opcodes.IF_ICMPGT;
                case ">" -> Opcodes.IF_ICMPLE;
                case ">=" -> Opcodes.IF_ICMPLT;
                default -> throw new IllegalArgumentException("Unsupported comparison operator: " + operator);
            };
        }
        return switch (operator) {
            case "==" -> Opcodes.IFNE;
            case "!=" -> Opcodes.IFEQ;
            case "<" -> Opcodes.IFGE;
            case "<=" -> Opcodes.IFGT;
            case ">" -> Opcodes.IFLE;
            case ">=" -> Opcodes.IFLT;
            default -> throw new IllegalArgumentException("Unsupported comparison operator: " + operator);
        };
    }

    private int comparisonTrueJumpOpcode(String operator, Type comparisonType) {
        if (isIntLike(comparisonType)) {
            return switch (operator) {
                case "==" -> Opcodes.IF_ICMPEQ;
                case "!=" -> Opcodes.IF_ICMPNE;
                case "<" -> Opcodes.IF_ICMPLT;
                case "<=" -> Opcodes.IF_ICMPLE;
                case ">" -> Opcodes.IF_ICMPGT;
                case ">=" -> Opcodes.IF_ICMPGE;
                default -> throw new IllegalArgumentException("Unsupported comparison operator: " + operator);
            };
        }
        return switch (operator) {
            case "==" -> Opcodes.IFEQ;
            case "!=" -> Opcodes.IFNE;
            case "<" -> Opcodes.IFLT;
            case "<=" -> Opcodes.IFLE;
            case ">" -> Opcodes.IFGT;
            case ">=" -> Opcodes.IFGE;
            default -> throw new IllegalArgumentException("Unsupported comparison operator: " + operator);
        };
    }

    private int floatCompareOpcode(String operator) {
        return switch (operator) {
            case "<", "<=" -> Opcodes.FCMPG;
            case ">", ">=" -> Opcodes.FCMPL;
            case "==", "!=" -> Opcodes.FCMPG;
            default -> throw new IllegalArgumentException("Unsupported comparison operator: " + operator);
        };
    }

    private int doubleCompareOpcode(String operator) {
        return switch (operator) {
            case "<", "<=" -> Opcodes.DCMPG;
            case ">", ">=" -> Opcodes.DCMPL;
            case "==", "!=" -> Opcodes.DCMPG;
            default -> throw new IllegalArgumentException("Unsupported comparison operator: " + operator);
        };
    }

    private Type comparisonType(GpuAsmCondition.Comparison comparison, LoweringContext context) {
        return expressionType(comparison.left(), context);
    }

    private Type expressionType(GpuAsmExpr expression, LoweringContext context) {
        if (expression instanceof GpuAsmExpr.IntLiteral) {
            return Type.INT_TYPE;
        }
        if (expression instanceof GpuAsmExpr.LongLiteral) {
            return Type.LONG_TYPE;
        }
        if (expression instanceof GpuAsmExpr.FloatLiteral) {
            return Type.FLOAT_TYPE;
        }
        if (expression instanceof GpuAsmExpr.DoubleLiteral) {
            return Type.DOUBLE_TYPE;
        }
        if (expression instanceof GpuAsmExpr.LocalRef) {
            GpuAsmExpr.LocalRef localRef = (GpuAsmExpr.LocalRef) expression;
            return requireLocal(context, localRef.name()).type();
        }
        if (expression instanceof GpuAsmExpr.ArrayLoad) {
            GpuAsmExpr.ArrayLoad arrayLoad = (GpuAsmExpr.ArrayLoad) expression;
            return arrayLoad.elementType();
        }
        if (expression instanceof GpuAsmExpr.Binary) {
            GpuAsmExpr.Binary binary = (GpuAsmExpr.Binary) expression;
            return binary.resultType();
        }
        if (expression instanceof GpuAsmExpr.Unary) {
            GpuAsmExpr.Unary unary = (GpuAsmExpr.Unary) expression;
            return expressionType(unary.expression(), context);
        }
        if (expression instanceof GpuAsmExpr.Cast) {
            GpuAsmExpr.Cast cast = (GpuAsmExpr.Cast) expression;
            return cast.targetType();
        }
        if (expression instanceof GpuAsmExpr.StaticCall) {
            GpuAsmExpr.StaticCall staticCall = (GpuAsmExpr.StaticCall) expression;
            return staticCall.returnType();
        }
        throw new IllegalArgumentException("Unsupported experimental GPU ASM expression: " + expression.getClass().getName());
    }

    private boolean isIntLike(Type type) {
        return switch (type.getSort()) {
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> true;
            default -> false;
        };
    }

    private LocalBinding requireLocal(LoweringContext context, String name) {
        LocalBinding binding = context.localsByName.get(name);
        if (binding == null) {
            throw new IllegalArgumentException("Unknown local in experimental GPU ASM AST: " + name);
        }
        return binding;
    }

    private record LocalBinding(int slot, Type type) {
    }

    private static final class LoweringContext {
        private final GpuFriendlyAsmMethodBuilder builder;
        private final Map<String, LocalBinding> localsByName;

        private LoweringContext(GpuFriendlyAsmMethodBuilder builder) {
            this(builder, new LinkedHashMap<>());
        }

        private LoweringContext(GpuFriendlyAsmMethodBuilder builder, Map<String, LocalBinding> localsByName) {
            this.builder = builder;
            this.localsByName = localsByName;
        }
    }
}
