package edu.kit.kastel.vads.compiler.parser.symbol;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class IdentName implements Name {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    private final String value;
    private final int id;
    public IdentName(String value) {
        this.value = value;
        this.id = NEXT_ID.getAndIncrement();
    }
    @Override public String asString() { return value; }
    @Override public String toString() { return value + "#" + id; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IdentName identName = (IdentName) o;
        return Objects.equals(value, identName.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
}