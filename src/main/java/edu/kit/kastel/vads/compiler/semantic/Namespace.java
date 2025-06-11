package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;

public class Namespace<T> {

    private final Map<Name, T> content;

    public Namespace() {
        this.content = new HashMap<>();
    }

    public Namespace<T> fork() {
        Namespace<T> forked = new Namespace<>();
        forked.content.putAll(this.content);
        return forked;
    }

    public void put(NameTree name, T value, BinaryOperator<T> merger) {
        this.content.merge(name.name(), value, merger);
    }

    public @Nullable T get(NameTree name) {
        return this.content.get(name.name());
    }
}
