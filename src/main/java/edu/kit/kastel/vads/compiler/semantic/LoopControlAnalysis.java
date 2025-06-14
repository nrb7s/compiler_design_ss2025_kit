package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

/// Checks that break and continue statements appear inside loops.
public class LoopControlAnalysis implements NoOpVisitor<Integer> {

    @Override
    public Unit visit(ProgramTree programTree, Integer depth) {
        for (FunctionTree f : programTree.topLevelTrees()) {
            f.accept(this, depth);
        }
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(FunctionTree functionTree, Integer depth) {
        functionTree.body().accept(this, depth);
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(BlockTree blockTree, Integer depth) {
        for (StatementTree stmt : blockTree.statements()) {
            stmt.accept(this, depth);
        }
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(IfTree ifTree, Integer depth) {
        ifTree.condition().accept(this, depth);
        ifTree.thenBranch().accept(this, depth);
        if (ifTree.elseBranch() != null) {
            ifTree.elseBranch().accept(this, depth);
        }
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(WhileLoopTree whileLoopTree, Integer depth) {
        whileLoopTree.condition().accept(this, depth);
        whileLoopTree.body().accept(this, depth + 1);
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(ForLoopTree forLoopTree, Integer depth) {
        if (forLoopTree.init() != null) {
            forLoopTree.init().accept(this, depth);
        }
        if (forLoopTree.condition() != null) {
            forLoopTree.condition().accept(this, depth);
        }
        if (forLoopTree.step() != null) {
            forLoopTree.step().accept(this, depth + 1);
        }
        forLoopTree.body().accept(this, depth + 1);
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(BreakTree breakTree, Integer depth) {
        if (depth == 0) {
            throw new SemanticException("break outside of loop");
        }
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(ContinueTree continueTree, Integer depth) {
        if (depth == 0) {
            throw new SemanticException("continue outside of loop");
        }
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(ExpressionStatementTree expressionStatementTree, Integer depth) {
        expressionStatementTree.expr().accept(this, depth);
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(ReturnTree returnTree, Integer depth) {
        returnTree.expression().accept(this, depth);
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(AssignmentTree assignmentTree, Integer depth) {
        assignmentTree.expression().accept(this, depth);
        return Unit.INSTANCE;
    }
}