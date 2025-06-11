package edu.kit.kastel.vads.compiler.ir.node;

public final class LogicalNotNode extends Node {
    public static final int OPERAND = 0;

    public LogicalNotNode(Block block, Node operand) {
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
