package edu.kit.kastel.vads.compiler.ir.node;

import edu.kit.kastel.vads.compiler.ir.IrGraph;
import java.util.ArrayList;
import java.util.List;

public final class Block extends Node {
    private static int nextId = 0;
    private final int id;
    private final List<Node> nodes = new ArrayList<>();
    private final List<Block> cfgPredecessors = new ArrayList<>();
    private final List<Block> cfgSuccessors = new ArrayList<>();

    public List<Block> cfgPredecessors() {
        return cfgPredecessors;
    }

    public List<Block> cfgSuccessors() {
        return cfgSuccessors;
    }

    public void addCfgSuccessor(Block block) {
        cfgSuccessors.add(block);
        block.cfgPredecessors.add(this);
    }

    public Block(IrGraph graph) {
        super(graph);
        this.id = nextId++;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return "L" + getId();
    }

    public void addNode(Node node) {
        nodes.add(node);
    }

    public List<Node> nodes() {
        return nodes;
    }
}
