package edu.kit.kastel.vads.compiler.ir.node;

public final class StoreNode extends Node {
    public static final int VALUE = 0;
    public StoreNode(Block block, Node value) {
        super(block, value);
    }
}