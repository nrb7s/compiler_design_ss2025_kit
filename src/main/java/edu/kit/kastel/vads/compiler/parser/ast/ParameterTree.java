package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

/** Represents a single function parameter */
public record ParameterTree(TypeTree type, NameTree name) implements Tree {
    @Override
    public Span span() {
        return new Span.SimpleSpan(type.span().start(), name.span().end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}