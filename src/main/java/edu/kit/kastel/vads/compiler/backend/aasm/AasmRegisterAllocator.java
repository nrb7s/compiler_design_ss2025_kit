package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AasmRegisterAllocator implements RegisterAllocator {
    private int id;
    private final Map<Node, Register> registers = new HashMap<>();

    @Override
    public Map<Node, Register> allocateRegisters(IrGraph graph) {
        int id = 0;
        Map<Node, Register> registers = new HashMap<>();
        Set<Node> allNodes = new HashSet<>();
        for (Block block : graph.blocks()) {
            for (Node node : block.nodes()) {
                if (needsRegister(node)) {
                    registers.put(node, new VirtualRegister(id++));
                }
            }
        }
        for (Node node : allNodes) {
            if (needsRegister(node) && !registers.containsKey(node)) {
                System.out.println("Warning: node not assigned register: " + node + " in block "
                        + ((node.block() != null) ? node.block().getId() : "unknown"));
            }
        }
        return Map.copyOf(registers);
    }

    private void scan(Node node, Set<Node> visited) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited);
            }
        }
        if (needsRegister(node)) {
            this.registers.put(node, new VirtualRegister(this.id++));
        }
    }

    private static boolean needsRegister(Node node) {
        return !(node instanceof ProjNode || node instanceof StartNode || node instanceof Block || node instanceof ReturnNode);
    }
}
