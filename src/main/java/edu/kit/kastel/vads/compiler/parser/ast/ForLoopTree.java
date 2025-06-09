package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record ForLoopTree(
        StatementTree init,
        ExpressionTree condition,
        StatementTree step,
        StatementTree body
) implements StatementTree {
    @Override
    public Span span() {
        Span s = body.span();
        if (init != null) s = init.span().merge(s);
        if (condition != null) s = condition.span().merge(s);
        if (step != null) s = step.span().merge(s);
        return s;
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}