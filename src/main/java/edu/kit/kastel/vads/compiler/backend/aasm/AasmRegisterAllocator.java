package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;

import java.util.*;

public class AasmRegisterAllocator implements RegisterAllocator {
    private int id;
    private final Map<Node, Register> registers = new HashMap<>();

    @Override
    public Map<Node, Register> allocateRegisters(IrGraph graph) {
        this.id = 0;
        this.registers.clear();
        for (Block block : getAllBlocks(graph)) {
            for (Node node : block.nodes()) {
                if (needsRegister(node)) {
                    this.registers.put(node, new VirtualRegister(this.id++));
                }
            }
        }
        return Map.copyOf(this.registers);
    }

    private Set<Block> getAllBlocks(IrGraph graph) {
        Set<Block> all = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        queue.add(graph.startBlock());
        while (!queue.isEmpty()) {
            Block block = queue.poll();
            if (all.add(block)) {
                for (Block succ : block.cfgSuccessors()) {
                    queue.add(succ);
                }
            }
        }
        return all;
    }

    /*
    @Override
    public Map<Node, Register> allocateRegisters(IrGraph graph) {
        Set<Node> visited = new HashSet<>();
        visited.add(graph.endBlock());
        scan(graph.endBlock(), visited);
        return Map.copyOf(this.registers);
    }

     */

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
