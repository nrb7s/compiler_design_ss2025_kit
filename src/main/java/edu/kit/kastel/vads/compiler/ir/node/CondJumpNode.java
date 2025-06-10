package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;

public final class CondJumpNode extends Node {
    private final Node condition;
    private final Block trueTarget;
    private final Block falseTarget;

    public CondJumpNode(Block block, Node condition, Block trueTarget, Block falseTarget) {
        super(block);
        this.condition = condition;
        this.trueTarget = trueTarget;
        this.falseTarget = falseTarget;
    }

    public Node condition() {
        return condition;
    }

    public Block trueTarget() {
        return trueTarget;
    }

    public Block falseTarget() {
        return falseTarget;
    }
}