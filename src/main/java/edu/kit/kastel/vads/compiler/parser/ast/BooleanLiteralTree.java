package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

public record BooleanLiteralTree(boolean value, Span span) implements ExpressionTree {

    /**
     * true for “true”, false for “false”
     */
    @Override
    public boolean value() {
        return value;
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}
