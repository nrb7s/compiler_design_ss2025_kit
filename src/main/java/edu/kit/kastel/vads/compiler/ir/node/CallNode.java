package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.parser.symbol.Name;

import java.util.List;

/** Represents a function call. Predecessor 0 is the current side effect followed by arguments. */
public final class CallNode extends Node {
    public static final int SIDE_EFFECT = 0;
    private final Name callee;

    public CallNode(Block block, Name callee, Node sideEffect, List<Node> arguments) {
        super(block, buildPreds(sideEffect, arguments));
        this.callee = callee;
    }

    private static Node[] buildPreds(Node sideEffect, List<Node> args) {
        Node[] res = new Node[1 + args.size()];
        res[0] = sideEffect;
        for (int i = 0; i < args.size(); i++) {
            res[i + 1] = args.get(i);
        }
        return res;
    }

    public Name callee() { return callee; }

    public int argumentCount() { return predecessors().size() - 1; }

    public Node argument(int idx) { return predecessor(idx + 1); }

    @Override
    protected String info() {
        return callee.asString();
    }
}