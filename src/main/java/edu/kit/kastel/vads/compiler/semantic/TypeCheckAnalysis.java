package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class TypeCheckAnalysis implements NoOpVisitor<Namespace<BasicType>> {

    @Override
    public Unit visit(DeclarationTree declarationTree, Namespace<BasicType> data) {
        BasicType type = (BasicType) declarationTree.type().type();
        data.put(declarationTree.name(), type, (existing, replacement) -> existing);
        return NoOpVisitor.super.visit(declarationTree, data);
    }

    @Override
    public Unit visit(AssignmentTree assignmentTree, Namespace<BasicType> data) {
        if (assignmentTree.lValue() instanceof LValueIdentTree ident) {
            BasicType expected = data.get(ident.name());
            if (expected == null) {
                throw new SemanticException("variable " + ident.name() + " not declared");
            }
            BasicType exprType = expressionType(assignmentTree.expression(), data);
            if (exprType != expected) {
                throw new SemanticException(
                        "cannot assign " + exprType.asString() + " to " + expected.asString());
            }
        }
        return NoOpVisitor.super.visit(assignmentTree, data);
    }

    @Override
    public Unit visit(ReturnTree returnTree, Namespace<BasicType> data) {
        expressionType(returnTree.expression(), data);
        return NoOpVisitor.super.visit(returnTree, data);
    }

    @Override
    public Unit visit(ExpressionStatementTree expressionStatementTree, Namespace<BasicType> data) {
        expressionType(expressionStatementTree.expr(), data);
        return NoOpVisitor.super.visit(expressionStatementTree, data);
    }

    @Override
    public Unit visit(IfTree ifTree, Namespace<BasicType> data) {
        BasicType cond = expressionType(ifTree.condition(), data);
        if (cond != BasicType.BOOL) {
            throw new SemanticException("if condition must be bool");
        }
        ifTree.thenBranch().accept(this, data.fork());
        if (ifTree.elseBranch() != null) {
            ifTree.elseBranch().accept(this, data.fork());
        }
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(WhileLoopTree whileLoopTree, Namespace<BasicType> data) {
        BasicType cond = expressionType(whileLoopTree.condition(), data);
        if (cond != BasicType.BOOL) {
            throw new SemanticException("while condition must be bool");
        }
        whileLoopTree.body().accept(this, data.fork());
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(ForLoopTree forLoopTree, Namespace<BasicType> data) {
        Namespace<BasicType> scope = data.fork();
        if (forLoopTree.init() != null) {
            forLoopTree.init().accept(this, scope);
        }
        if (forLoopTree.condition() != null) {
            BasicType cond = expressionType(forLoopTree.condition(), scope);
            if (cond != BasicType.BOOL) {
                throw new SemanticException("for condition must be bool");
            }
        }
        if (forLoopTree.step() != null) {
            forLoopTree.step().accept(this, scope);
        }
        forLoopTree.body().accept(this, scope);
        return Unit.INSTANCE;
    }

    private BasicType expressionType(ExpressionTree expr, Namespace<BasicType> data) {
        return switch (expr) {
            case LiteralTree l -> BasicType.INT;
            case BooleanLiteralTree b -> BasicType.BOOL;
            case IdentExpressionTree id -> {
                BasicType t = data.get(id.name());
                if (t == null) {
                    throw new SemanticException("variable " + id.name() + " not declared");
                }
                yield t;
            }
            case NegateTree neg -> {
                BasicType op = expressionType(neg.expression(), data);
                if (op != BasicType.INT) {
                    throw new SemanticException("unary - expects int");
                }
                yield BasicType.INT;
            }
            case LogicalNotTree not -> {
                BasicType op = expressionType(not.operand(), data);
                if (op != BasicType.BOOL) {
                    throw new SemanticException("! expects bool");
                }
                yield BasicType.BOOL;
            }
            case BitwiseNotTree bit -> {
                BasicType op = expressionType(bit.operand(), data);
                if (op != BasicType.INT) {
                    throw new SemanticException("~ expects int");
                }
                yield BasicType.INT;
            }
            case ConditionalTree cond -> {
                BasicType c = expressionType(cond.condition(), data);
                if (c != BasicType.BOOL) {
                    throw new SemanticException("condition must be bool");
                }
                BasicType t = expressionType(cond.thenExpr(), data);
                BasicType e = expressionType(cond.elseExpr(), data);
                if (t != e) {
                    throw new SemanticException("types in ?: must match");
                }
                yield t;
            }
            case BinaryOperationTree bin -> binaryType(bin, data);
        };
    }

    private BasicType binaryType(BinaryOperationTree bin, Namespace<BasicType> data) {
        BasicType lhs = expressionType(bin.lhs(), data);
        BasicType rhs = expressionType(bin.rhs(), data);
        OperatorType op = bin.operatorType();
        return switch (op) {
            case PLUS, MINUS, MUL, DIV, MOD,
                 LSHIFT, RSHIFT, SHL, SHR,
                 AND, OR, XOR,
                 ASSIGN_MINUS, ASSIGN_PLUS, ASSIGN_MUL, ASSIGN_DIV, ASSIGN_MOD,
                 ASSIGN_LSHIFT, ASSIGN_RSHIFT, ASSIGN_AND, ASSIGN_OR, ASSIGN_XOR -> {
                requireInt(lhs, rhs, op);
                yield BasicType.INT;
            }
            case EQ, NE, LT, LE, GT, GE -> {
                requireSame(lhs, rhs, op);
                yield BasicType.BOOL;
            }
            case ANDAND, OROR -> {
                requireBool(lhs, rhs, op);
                yield BasicType.BOOL;
            }
            default -> throw new SemanticException("unsupported operator " + op);
        };
    }

    private static void requireInt(BasicType lhs, BasicType rhs, OperatorType op) {
        if (lhs != BasicType.INT || rhs != BasicType.INT) {
            throw new SemanticException(op + " expects int operands");
        }
    }

    private static void requireBool(BasicType lhs, BasicType rhs, OperatorType op) {
        if (lhs != BasicType.BOOL || rhs != BasicType.BOOL) {
            throw new SemanticException(op + " expects bool operands");
        }
    }

    private static void requireSame(BasicType lhs, BasicType rhs, OperatorType op) {
        if (lhs != rhs) {
            throw new SemanticException(op + " operands must have same type");
        }
    }
}