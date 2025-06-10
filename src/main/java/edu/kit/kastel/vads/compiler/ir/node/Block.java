package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import java.util.ArrayList;
import java.util.List;

public final class Block extends Node {
    private final List<Node> nodes = new ArrayList<>();

    public Block(IrGraph graph) {
        super(graph);
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public List<Node> nodes() {
        return nodes;
    }
}
