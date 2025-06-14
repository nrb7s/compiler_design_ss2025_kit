package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.backend.regalloc.RegisterAllocator;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.PhiElimination;
import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AasmRegisterAllocator implements RegisterAllocator {
    private int id;
    private final Map<Node, Register> registers = new HashMap<>();

    @Override
    public Map<Node, Register> allocateRegisters(IrGraph graph) {
        this.id = 0;
        this.registers.clear();
        Set<Node> visited = new HashSet<>();
        for (Block block : graph.blocks()) {
            for (Node node : block.nodes()) {
                scan(node, visited);
            }
        }
        return Map.copyOf(this.registers);
    }

    private void scan(Node node, Set<Node> visited) {
        if (!visited.add(node)) return;
        for (Node predecessor : node.predecessors()) {
            scan(predecessor, visited);
        }
        if (node instanceof PhiElimination.CopyNode copy) {
            Node src = predecessorSkipProj(copy, PhiElimination.CopyNode.SRC);
            Node dst = predecessorSkipProj(copy, PhiElimination.CopyNode.DST);
            Register r = registers.get(src);
            if (r == null) r = registers.get(dst);
            if (r == null) r = new VirtualRegister(this.id++);
            registers.put(src, r);
            registers.put(dst, r);
            // System.out.println("reg " + r + " for copy src=" + src + " dst=" + dst);
            return;
        }
        if (needsRegister(node) && !registers.containsKey(node)) {
            Register r = new VirtualRegister(this.id++);
            registers.put(node, r);
            // System.out.println("reg " + r + " for node " + node);
        }
    }

    private static boolean needsRegister(Node node) {
        return !(node instanceof ProjNode
                || node instanceof StartNode
                || node instanceof Block
                || node instanceof ReturnNode
                || node instanceof PhiElimination.CopyNode);
    }
}
