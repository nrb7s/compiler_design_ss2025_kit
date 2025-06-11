package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class Namespace<T> {
    private final Deque<Map<Name, T>> scopes = new ArrayDeque<>();

    public Namespace() { push(); }

    public void push() {
        scopes.push(new HashMap<>());
    }
    public void pop() {
        scopes.pop();
    }

    public void put(NameTree name, T value, java.util.function.BinaryOperator<T> merger) {
        Map<Name, T> current = scopes.peek();
        current.merge(name.name(), value, merger);
    }

    public @Nullable T get(NameTree name) {
        for (Map<Name, T> scope : scopes) {
            T value = scope.get(name.name());
            if (value != null) return value;
        }
        return null;
    }
}
