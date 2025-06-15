package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

    public void mergeExisting(Namespace<T> other, BinaryOperator<T> merger) {
        for (Map.Entry<Name, T> e : other.content.entrySet()) {
            if (this.content.containsKey(e.getKey())) {
                this.content.merge(e.getKey(), e.getValue(), merger);
            }
        }
    }

    public Set<Name> names() {
        return this.content.keySet();
    }

    public void put(NameTree name, T value, BinaryOperator<T> merger) {
        put(name.name(), value, merger);
    }

    public void put(Name name, T value, BinaryOperator<T> merger) {
        this.content.merge(name, value, merger);
    }

    public @Nullable T get(Name name) {
        return this.content.get(name);
    }

    public @Nullable T get(NameTree name) {
        return this.content.get(name.name());
    }
}
