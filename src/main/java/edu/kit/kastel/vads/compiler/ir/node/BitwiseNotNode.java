package edu.kit.kastel.vads.compiler.ir.node;

public final class BitwiseNotNode extends Node {
    public static final int OPERAND = 0;

    public BitwiseNotNode(Block block, Node operand) {
        super(block, operand);
    }

    public Node operand() {
        return predecessor(OPERAND);
    }

    @Override
    protected String info() {
        return operand().toString();
    }
}