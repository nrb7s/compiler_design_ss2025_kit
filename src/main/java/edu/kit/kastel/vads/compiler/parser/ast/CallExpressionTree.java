package edu.kit.kastel.vads.compiler.parser.ast;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

import java.util.List;

/** Represents a function call expression */
public record CallExpressionTree(NameTree callee, List<ExpressionTree> arguments) implements ExpressionTree {
    public CallExpressionTree {
        arguments = List.copyOf(arguments);
    }

    @Override
    public Span span() {
        if (arguments.isEmpty()) {
            return callee.span();
        }
        Span argSpan = arguments.getLast().span();
        return new Span.SimpleSpan(callee.span().start(), argSpan.end());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T data) {
        return visitor.visit(this, data);
    }
}