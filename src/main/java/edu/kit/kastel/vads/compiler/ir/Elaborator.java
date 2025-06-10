package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.lexer.KeywordType;
import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import java.util.List;
import java.util.ArrayList;

public final class Elaborator {

    public static FunctionTree elaborate(FunctionTree function) {
        StatementTree simplifiedBody = elaborateStmt(function.body());
        return new FunctionTree(
                function.returnType(),
                function.name(),
                toBlock(simplifiedBody)
        );
    }

    private static StatementTree elaborateStmt(StatementTree stmt) {
        return switch (stmt) {
            case ForLoopTree forLoop -> elaborateFor(forLoop);
            case BlockTree block -> elaborateBlock(block);
            case IfTree ifTree -> new IfTree(
                    elaborateExpr(ifTree.condition()),
                    elaborateStmt(ifTree.thenBranch()),
                    ifTree.elseBranch() == null ? null : elaborateStmt(ifTree.elseBranch())
            );
            case WhileLoopTree whileTree -> new WhileLoopTree(
                    elaborateExpr(whileTree.condition()),
                    elaborateStmt(whileTree.body())
            );
            case ReturnTree ret -> new ReturnTree(
                    elaborateExpr(ret.expression()),
                    ret.start()
            );
            case AssignmentTree assign -> new AssignmentTree(
                    assign.lValue(),
                    assign.operator(),
                    elaborateExpr(assign.expression())
            );
            case DeclarationTree decl -> elaborateDecl(decl);
            default -> stmt;
        };
    }

    private static StatementTree elaborateFor(ForLoopTree forLoop) {
        StatementTree init = forLoop.init();
        ExpressionTree cond = forLoop.condition();
        StatementTree step = forLoop.step();
        StatementTree body = forLoop.body();

        StatementTree loopBody = step == null
                ? elaborateStmt(body)
                : toBlock(List.of(
                elaborateStmt(body),
                elaborateStmt(step)
        ));

        WhileLoopTree whileLoop = new WhileLoopTree(
                cond == null
                        ? new LiteralTree("true", 0, Span.DUMMY)
                        : elaborateExpr(cond),
                loopBody
        );

        return init == null
                ? whileLoop
                : toBlock(List.of(elaborateStmt(init), whileLoop));
    }

    private static StatementTree elaborateDecl(DeclarationTree decl) {
        if (decl.initializer() == null) {
            return decl;
        }
        DeclarationTree noInit = new DeclarationTree(
                decl.type(),
                decl.name(),
                null
        );
        AssignmentTree initAssign = new AssignmentTree(
                new LValueIdentTree(decl.name()),
                new Operator(OperatorType.ASSIGN, decl.span()),
                elaborateExpr(decl.initializer())
        );
        return toBlock(List.of(noInit, initAssign));
    }

    private static BlockTree elaborateBlock(BlockTree block) {
        List<StatementTree> stmts = new ArrayList<>();
        for (StatementTree s : block.statements()) {
            StatementTree elaborated = elaborateStmt(s);
            if (elaborated instanceof BlockTree b) {
                stmts.addAll(b.statements());
            } else {
                stmts.add(elaborated);
            }
        }
        return new BlockTree(stmts, block.span());
    }

    private static ExpressionTree elaborateExpr(ExpressionTree expr) {
        return switch (expr) {
            case BinaryOperationTree bin when bin.operatorType() == OperatorType.ANDAND ->
                    new ConditionalTree(
                            elaborateExpr(bin.lhs()),
                            elaborateExpr(bin.rhs()),
                            new LiteralTree("false", 0, Span.DUMMY)
                    );
            case BinaryOperationTree bin when bin.operatorType() == OperatorType.OROR ->
                    new ConditionalTree(
                            elaborateExpr(bin.lhs()),
                            new LiteralTree("true", 0, Span.DUMMY),
                            elaborateExpr(bin.rhs())
                    );
            case BinaryOperationTree bin ->
                    new BinaryOperationTree(
                            elaborateExpr(bin.lhs()),
                            elaborateExpr(bin.rhs()),
                            bin.operatorType()
                    );
            case NegateTree neg ->
                    new NegateTree(
                            elaborateExpr(neg.expression()),
                            neg.span()
                    );
            case LogicalNotTree not ->
                    new LogicalNotTree(
                            elaborateExpr(not.operand())
                    );
            case BitwiseNotTree bit ->
                    new BitwiseNotTree(
                            elaborateExpr(bit.operand())
                    );
            case ConditionalTree cond ->
                    new ConditionalTree(
                            elaborateExpr(cond.condition()),
                            elaborateExpr(cond.thenExpr()),
                            elaborateExpr(cond.elseExpr())
                    );
            default -> expr;
        };
    }

    private static BlockTree toBlock(StatementTree stmt) {
        if (stmt instanceof BlockTree b) {
            return b;
        }
        return new BlockTree(List.of(stmt), stmt.span());
    }

    private static BlockTree toBlock(List<StatementTree> stmts) {
        Span span = stmts.get(0).span().merge(stmts.get(stmts.size() - 1).span());
        return new BlockTree(stmts, span);
    }
}