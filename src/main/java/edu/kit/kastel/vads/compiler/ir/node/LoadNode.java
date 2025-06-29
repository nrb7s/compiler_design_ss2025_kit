package edu.kit.kastel.vads.compiler.ir.node;

public final class LoadNode extends Node {
    public static final int SIDE_EFFECT = 0;

    public LoadNode(Block block, Node sideEffect) {
        super(block, sideEffect);
    }
}