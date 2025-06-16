package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;

public class Namespace<T> {

    private final @Nullable Namespace<T> parent;
    private final Map<Name, T> content;
    private final Set<Name> declaredHere;

    public Namespace() {
        this(null);
    }

    private Namespace(@Nullable Namespace<T> parent) {
        this.parent = parent;
        this.content = new HashMap<>();
        this.declaredHere = new HashSet<>();
    }

    public Namespace<T> fork() {
        return new Namespace<>(this);
    }

    public boolean isDeclaredLocally(NameTree name) {
        return this.declaredHere.contains(name.name());
    }

    public void declare(NameTree name, T value) {
        if (this.content.containsKey(name.name())) {
            throw new SemanticException("Variable " + name.name() + " already declared in this scope");
        }
        this.content.put(name.name(), value);
        this.declaredHere.add(name.name());
    }

    public void mergeExisting(Namespace<T> other, BinaryOperator<T> merger) {
        for (Map.Entry<Name, T> e : other.content.entrySet()) {
            if (this.content.containsKey(e.getKey())) {
                this.content.merge(e.getKey(), e.getValue(), merger);
            }
        }
        for (Name nameDeclaredInOther : other.declaredHere) {
            if (!this.content.containsKey(nameDeclaredInOther) && this.get(nameDeclaredInOther) == null) {
                this.content.put(nameDeclaredInOther, other.content.get(nameDeclaredInOther));
                // this.declaredHere.add(nameDeclaredInOther);
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
        T type = this.content.get(name);
        if (type != null) {
            return type;
        }
        if (this.parent != null) {
            return this.parent.get(name);
        }
        return null;
    }

    public @Nullable T get(NameTree name) {
        return this.content.get(name.name());
    }
}
