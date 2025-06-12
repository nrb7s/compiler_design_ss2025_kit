package edu.kit.kastel.vads.compiler.ir.node;

public final class AndNode extends BinaryOperationNode {
    public AndNode(Block b, Node l, Node r) { super(b, l, r); }

    @Override
    public boolean equals(Object obj) { return commutativeEquals(this, obj); }
    @Override
    public int hashCode()          { return commutativeHashCode(this); }
}
