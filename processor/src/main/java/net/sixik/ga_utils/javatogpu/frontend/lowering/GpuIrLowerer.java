package net.sixik.ga_utils.javatogpu.frontend.lowering;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import net.sixik.ga_utils.javatogpu.frontend.intrinsics.GpuIntrinsic;
import net.sixik.ga_utils.javatogpu.frontend.intrinsics.GpuIntrinsicDatabase;
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
import net.sixik.ga_utils.javatogpu.frontend.model.ParsedGpuMethod;

import java.util.List;
import java.util.Set;

public final class GpuIrLowerer {

    private static final Set<String> ALLOWED_BINARY_OPERATORS = Set.of(
            "+", "-", "*", "/", "%",
            "<", "<=", ">", ">=",
            "==", "!=",
            "&&", "||",
            "&", "|", "^",
            "<<", ">>"
    );

    private static final Set<String> ALLOWED_UNARY_OPERATORS = Set.of("!", "-", "~");

    private final GpuIntrinsicDatabase intrinsicDatabase;

    public GpuIrLowerer(GpuIntrinsicDatabase intrinsicDatabase) {
        this.intrinsicDatabase = intrinsicDatabase;
    }

    public GpuIrMethod lower(ParsedGpuMethod method) {
        List<GpuIrStatement> statements = method.declaration().getBody()
                .orElseThrow(() -> new IllegalArgumentException("GPU method must have a body"))
                .getStatements()
                .stream()
                .map(this::lowerStatement)
                .toList();

        return new GpuIrMethod(method.name(), statements);
    }

    private GpuIrStatement lowerStatement(com.github.javaparser.ast.stmt.Statement statement) {
        if (statement instanceof ForStmt forStmt) {
            return lowerForLoop(forStmt);
        }

        if (statement instanceof IfStmt ifStmt) {
            return lowerIf(ifStmt);
        }

        if (statement instanceof ExpressionStmt expressionStmt) {
            Expression expression = expressionStmt.getExpression();

            if (expression instanceof VariableDeclarationExpr declarationExpr) {
                VariableDeclarator variable = declarationExpr.getVariables().get(0);
                GpuIrExpression initializer = variable.getInitializer()
                        .map(this::lowerExpression)
                        .orElseThrow(() -> new IllegalArgumentException("Variable declaration must have an initializer"));
                return new GpuIrVariableDeclaration(
                        variable.getTypeAsString(),
                        variable.getNameAsString(),
                        initializer
                );
            }

            if (expression instanceof AssignExpr assignExpr) {
                return new GpuIrAssignment(
                        lowerExpression(assignExpr.getTarget()),
                        lowerExpression(assignExpr.getValue())
                );
            }
        }

        throw new IllegalArgumentException("Unsupported statement for first lowering pass: " + statement);
    }

    private GpuIrIf lowerIf(IfStmt ifStmt) {
        if (!ifStmt.getThenStmt().isBlockStmt()) {
            throw new IllegalArgumentException("If then-branch must use braces");
        }

        List<GpuIrStatement> thenBranch = ifStmt.getThenStmt().asBlockStmt().getStatements().stream()
                .map(this::lowerStatement)
                .toList();
        List<GpuIrStatement> elseBranch = ifStmt.getElseStmt()
                .map(statement -> {
                    if (statement.isIfStmt()) {
                        return List.<GpuIrStatement>of(lowerIf(statement.asIfStmt()));
                    }
                    if (statement.isBlockStmt()) {
                        return statement.asBlockStmt().getStatements().stream()
                                .map(this::lowerStatement)
                                .toList();
                    }
                    throw new IllegalArgumentException("If else-branch must use braces or else-if");
                })
                .orElse(List.of());

        return new GpuIrIf(
                lowerExpression(ifStmt.getCondition()),
                thenBranch,
                elseBranch
        );
    }

    private GpuIrForLoop lowerForLoop(ForStmt forStmt) {
        if (forStmt.getInitialization().size() != 1) {
            throw new IllegalArgumentException("Only single-initializer for loops are supported");
        }
        if (forStmt.getCompare().isEmpty()) {
            throw new IllegalArgumentException("For loop compare expression is required");
        }
        if (forStmt.getUpdate().size() != 1) {
            throw new IllegalArgumentException("Only single-update for loops are supported");
        }

        GpuIrStatement initializer = lowerForInitializer(forStmt.getInitialization().get(0));
        GpuIrExpression condition = lowerExpression(forStmt.getCompare().orElseThrow());
        GpuIrStatement update = lowerForUpdate(forStmt.getUpdate().get(0));
        List<GpuIrStatement> body = forStmt.getBody().asBlockStmt().getStatements().stream()
                .map(this::lowerStatement)
                .toList();

        return new GpuIrForLoop(initializer, condition, update, body);
    }

    private GpuIrStatement lowerForInitializer(Expression expression) {
        if (expression instanceof VariableDeclarationExpr declarationExpr) {
            VariableDeclarator variable = declarationExpr.getVariables().get(0);
            GpuIrExpression initializer = variable.getInitializer()
                    .map(this::lowerExpression)
                    .orElseThrow(() -> new IllegalArgumentException("For initializer must declare a value"));
            return new GpuIrVariableDeclaration(
                    variable.getTypeAsString(),
                    variable.getNameAsString(),
                    initializer
            );
        }

        throw new IllegalArgumentException("Unsupported for initializer: " + expression);
    }

    private GpuIrStatement lowerForUpdate(Expression expression) {
        if (expression instanceof UnaryExpr unaryExpr
                && (unaryExpr.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                || unaryExpr.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT)) {
            GpuIrExpression target = lowerExpression(unaryExpr.getExpression());
            return new GpuIrAssignment(
                    target,
                    new GpuIrBinary(
                            "+",
                            target,
                            new GpuIrLiteral("1")
                    )
            );
        }

        if (expression instanceof AssignExpr assignExpr) {
            return new GpuIrAssignment(
                    lowerExpression(assignExpr.getTarget()),
                    lowerExpression(assignExpr.getValue())
            );
        }

        throw new IllegalArgumentException("Unsupported for update: " + expression);
    }

    private GpuIrExpression lowerExpression(Expression expression) {
        if (expression instanceof NameExpr nameExpr) {
            return new GpuIrVariableRef(nameExpr.getNameAsString());
        }

        if (expression instanceof IntegerLiteralExpr literalExpr) {
            return new GpuIrLiteral(literalExpr.getValue());
        }

        if (expression instanceof DoubleLiteralExpr literalExpr) {
            return new GpuIrLiteral(literalExpr.toString());
        }

        if (expression instanceof EnclosedExpr enclosedExpr) {
            return lowerExpression(enclosedExpr.getInner());
        }

        if (expression instanceof ArrayAccessExpr arrayAccessExpr) {
            return new GpuIrArrayAccess(
                    arrayAccessExpr.getName().toString(),
                    lowerExpression(arrayAccessExpr.getIndex())
            );
        }

        if (expression instanceof BinaryExpr binaryExpr) {
            if (!ALLOWED_BINARY_OPERATORS.contains(binaryExpr.getOperator().asString())) {
                throw new IllegalArgumentException("Unsupported binary operator for lowering: " + binaryExpr.getOperator().asString());
            }
            return new GpuIrBinary(
                    binaryExpr.getOperator().asString(),
                    lowerExpression(binaryExpr.getLeft()),
                    lowerExpression(binaryExpr.getRight())
            );
        }

        if (expression instanceof ConditionalExpr conditionalExpr) {
            return new GpuIrTernary(
                    lowerExpression(conditionalExpr.getCondition()),
                    lowerExpression(conditionalExpr.getThenExpr()),
                    lowerExpression(conditionalExpr.getElseExpr())
            );
        }

        if (expression instanceof UnaryExpr unaryExpr) {
            if (ALLOWED_UNARY_OPERATORS.contains(unaryExpr.getOperator().asString())) {
                return new GpuIrUnary(
                        unaryExpr.getOperator().asString(),
                        lowerExpression(unaryExpr.getExpression())
                );
            }
        }

        if (expression instanceof MethodCallExpr methodCallExpr) {
            String owner = methodCallExpr.getScope()
                    .map(Expression::toString)
                    .orElseThrow(() -> new IllegalArgumentException("Intrinsic call must have an explicit owner"));
            GpuIntrinsic intrinsic = intrinsicDatabase.require(owner, methodCallExpr.getNameAsString(), methodCallExpr.getArguments().size());
            return new GpuIrIntrinsicCall(
                    intrinsic.backendName(),
                    intrinsic.resultType(),
                    methodCallExpr.getArguments().stream().map(this::lowerExpression).toList()
            );
        }

        throw new IllegalArgumentException("Unsupported expression for first lowering pass: " + expression);
    }
}
