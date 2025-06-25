package edu.kit.kastel.vads.compiler.ir.node;

import java.util.ArrayList;
import java.util.List;

public final class Phi extends Node {
    public record Input(Block block, Node node) {}
    private final List<Input> inputs = new ArrayList<>();

    public Phi(Block block) {
        super(block);
    }

    public void addInput(Block pred, Node value) {
        addPredecessor(value);
        inputs.add(new Input(pred, value));
    }

    public Node getInput(Block pred) {
        for (Input in : inputs) {
            if (in.block() == pred) {
                return in.node();
            }
        }
        return null;
    }

    public void appendOperand(Node node) {
        addPredecessor(node);
    }

    public List<Input> inputs() {
        return List.copyOf(inputs);
    }
}
