package edu.kit.kastel.vads.compiler.ir.node;

public final class StoreNode extends Node {
    public static final int SIDE_EFFECT = 0;
    public static final int VALUE = 1;

    public StoreNode(Block block, Node sideEffect, Node value) {
        super(block, sideEffect, value);
    }
}