package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record IfTree(
        ExpressionTree condition,
        StatementTree thenBranch,
        StatementTree elseBranch) implements StatementTree {
    @Override
    public Span span() {
        if (elseBranch != null) {
            return condition.span().merge(elseBranch.span());
        }
        return condition.span().merge(thenBranch.span());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}