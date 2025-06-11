package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.ast.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class IntegerLiteralRangeAnalysis implements NoOpVisitor<Namespace<Void>> {

    @Override
    public Unit visit(LiteralTree literalTree, Namespace<Void> data) {
      literalTree.parseValue()
          .orElseThrow(
              () -> new SemanticException("invalid integer literal " + literalTree.value())
          );
        return NoOpVisitor.super.visit(literalTree, data);
    }

    @Override
    public Unit visit(IfTree ifTree, Namespace<Void> data) {
        ifTree.condition().accept(this, data);
        ifTree.thenBranch().accept(this, data);
        if (ifTree.elseBranch() != null) {
            ifTree.elseBranch().accept(this, data);
        }
        return NoOpVisitor.super.visit(ifTree, data);
    }

    @Override
    public Unit visit(WhileLoopTree whileLoopTree, Namespace<Void> data) {
        whileLoopTree.condition().accept(this, data);
        whileLoopTree.body().accept(this, data);
        return NoOpVisitor.super.visit(whileLoopTree, data);
    }

    @Override
    public Unit visit(ForLoopTree forLoopTree, Namespace<Void> data) {
        if (forLoopTree.init() != null) {
            forLoopTree.init().accept(this, data);
        }
        if (forLoopTree.condition() != null) {
            forLoopTree.condition().accept(this, data);
        }
        if (forLoopTree.step() != null) {
            forLoopTree.step().accept(this, data);
        }
        forLoopTree.body().accept(this, data);
        return NoOpVisitor.super.visit(forLoopTree, data);
    }

    @Override
    public Unit visit(BreakTree breakTree, Namespace<Void> data) {
        return NoOpVisitor.super.visit(breakTree, data);
    }

    @Override
    public Unit visit(ContinueTree continueTree, Namespace<Void> data) {
        return NoOpVisitor.super.visit(continueTree, data);
    }

    @Override
    public Unit visit(ConditionalTree conditionalTree, Namespace<Void> data) {
        conditionalTree.condition().accept(this, data);
        conditionalTree.thenExpr().accept(this, data);
        conditionalTree.elseExpr().accept(this, data);
        return NoOpVisitor.super.visit(conditionalTree, data);
    }

    @Override
    public Unit visit(LogicalNotTree logicalNotTree, Namespace<Void> data) {
        logicalNotTree.operand().accept(this, data);
        return NoOpVisitor.super.visit(logicalNotTree, data);
    }

    @Override
    public Unit visit(BitwiseNotTree bitwiseNotTree, Namespace<Void> data) {
        bitwiseNotTree.operand().accept(this, data);
        return NoOpVisitor.super.visit(bitwiseNotTree, data);
    }
}
