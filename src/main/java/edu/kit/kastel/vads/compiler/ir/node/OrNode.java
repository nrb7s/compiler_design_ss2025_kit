package edu.kit.kastel.vads.compiler.ir.node;

public final class OrNode extends BinaryOperationNode {
    public OrNode(Block b, Node l, Node r) { super(b, l, r); }

    @Override
    public boolean equals(Object obj) { return commutativeEquals(this, obj); }
    @Override
    public int hashCode()          { return commutativeHashCode(this); }
}
