package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import java.util.ArrayList;
import java.util.List;

public final class Block extends Node {
    private static int nextId = 0;
    private final int id;
    private final List<Node> nodes = new ArrayList<>();

    public Block(IrGraph graph) {
        super(graph);
        this.id = nextId++;
    }

    public int getId() {
        return id;
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public List<Node> nodes() {
        return nodes;
    }
}
