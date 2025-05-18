package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.node.AddNode;
import edu.kit.kastel.vads.compiler.ir.node.BinaryOperationNode;
import edu.kit.kastel.vads.compiler.ir.node.Block;
import edu.kit.kastel.vads.compiler.ir.node.ConstIntNode;
import edu.kit.kastel.vads.compiler.ir.node.DivNode;
import edu.kit.kastel.vads.compiler.ir.node.ModNode;
import edu.kit.kastel.vads.compiler.ir.node.MulNode;
import edu.kit.kastel.vads.compiler.ir.node.Node;
import edu.kit.kastel.vads.compiler.ir.node.Phi;
import edu.kit.kastel.vads.compiler.ir.node.ProjNode;
import edu.kit.kastel.vads.compiler.ir.node.ReturnNode;
import edu.kit.kastel.vads.compiler.ir.node.StartNode;
import edu.kit.kastel.vads.compiler.ir.node.SubNode;

import java.util.*;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class CodeGenerator {
    private static final int PHYS_REG_COUNT = 6;
    private static final String[] PHYS_REGS = {
            "%eax", "%ebx", "%ecx", "%edx", "%esi", "%edi"
    };

    public String generateCode(List<IrGraph> program) {
        StringBuilder builder = new StringBuilder();
        for (IrGraph graph : program) {
            AasmRegisterAllocator allocator = new AasmRegisterAllocator();
            Map<Node, Register> registers = allocator.allocateRegisters(graph);
            builder.append("function ")
                .append(graph.name())
                .append(" {\n");
            generateForGraph(graph, builder, registers);
            builder.append("}");
        }
        return builder.toString();
    }

    public String generateAssembly(List<IrGraph> program) { // Using AT&T style instead of Intel style to meet the requirement of gcc
        StringBuilder builder = new StringBuilder();
        builder.append(".section .note.GNU-stack,\"\",@progbits\n\t.text\n");

        for (IrGraph graph : program) {
            AasmRegisterAllocator allocator = new AasmRegisterAllocator();
            Map<Node, Register> registers = allocator.allocateRegisters(graph);

            Set<Integer> spillIds = new TreeSet<>();
            for (Register r : registers.values()) {
                int id = ((VirtualRegister) r).id();
                if (id >= PHYS_REG_COUNT) spillIds.add(id);
            }
            Map<Integer,Integer> spillOffset = new HashMap<>();
            int index = 1;
            for (int id : spillIds) {
                spillOffset.put(id, index * 4); // 4 bytes per spill slot
                index++;
            }
            int totalSpillBytes = spillOffset.size() * 4;

            // Prologue and reserve stack for spilling
            builder.append("\t.globl ").append(graph.name()).append("\n");
            builder.append(graph.name()).append(":\n");
            builder.append("\tpushl %ebp\n");
            builder.append("\tmovl %esp, %ebp\n");
            if (totalSpillBytes > 0) {
                builder.append("\tsubl $")
                        .append(totalSpillBytes)
                        .append(", %esp\n");
            }

            generateAssemblyForGraph(graph, builder, registers, spillOffset);

            // Epilogue
            // builder.append("\tpopp %rbp\n");
            // builder.append("\tleave\n") // leave = movl %ebp, %esp then popl %ebp
            builder.append("\tmovl %ebp, %esp\n")
                    .append("\tpopl %ebp\n")
                    .append("\tret\n\n");
        }
        return builder.toString();
    }

    private void generateAssemblyForGraph(IrGraph graph, StringBuilder builder, Map<Node, Register> registers, Map<Integer,Integer> spillOffset) {
        Set<Node> visited = new HashSet<>();
        scanAsm(graph.endBlock(), visited, builder, registers, spillOffset);
    }

    private void scanAsm(Node node, Set<Node> visited, StringBuilder builder, Map<Node, Register> registers, Map<Integer,Integer> spillOffset) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scanAsm(predecessor, visited, builder, registers, spillOffset);
            }
        }

        switch (node) {
            case AddNode add -> binaryAsm(builder, registers, add, "addl", spillOffset);
            case SubNode sub -> binaryAsm(builder, registers, sub, "subl", spillOffset);
            case MulNode mul -> binaryAsm(builder, registers, mul, "imull", spillOffset);  // 32-bit, imulq for 64-bit
            case DivNode div -> divide(builder, registers, div, spillOffset);
            case ModNode mod -> mod(builder, registers, mod, spillOffset);
            case ReturnNode r -> {
                Node res = predecessorSkipProj(r, ReturnNode.RESULT);
                builder.append("\tmovl ")
                        .append(regAllocate(res, registers, spillOffset))
                        .append(", %eax\n");
            }
            case ConstIntNode c -> {
                String dest = regAllocate(c, registers, spillOffset);
                builder.append("\tmovl $").append(c.value())
                        .append(", ").append(dest)
                        .append("\n");
            }
            case Phi _ -> throw new UnsupportedOperationException("phi");
            case Block _, ProjNode _, StartNode _ -> {
                // do nothing, skip line break
                return;
            }
        }
    }

    private void generateForGraph(IrGraph graph, StringBuilder builder, Map<Node, Register> registers) {
        Set<Node> visited = new HashSet<>();
        scan(graph.endBlock(), visited, builder, registers);
    }

    private void scan(Node node, Set<Node> visited, StringBuilder builder, Map<Node, Register> registers) {
        for (Node predecessor : node.predecessors()) {
            if (visited.add(predecessor)) {
                scan(predecessor, visited, builder, registers);
            }
        }

        switch (node) {
            case AddNode add -> binary(builder, registers, add, "add");
            case SubNode sub -> binary(builder, registers, sub, "sub");
            case MulNode mul -> binary(builder, registers, mul, "mul");
            case DivNode div -> binary(builder, registers, div, "div");
            case ModNode mod -> binary(builder, registers, mod, "mod");
            case ReturnNode r -> builder.repeat(" ", 2).append("ret ")
                .append(registers.get(predecessorSkipProj(r, ReturnNode.RESULT)));
            case ConstIntNode c -> builder.repeat(" ", 2)
                .append(registers.get(c))
                .append(" = const ")
                .append(c.value());
            case Phi _ -> throw new UnsupportedOperationException("phi");
            case Block _, ProjNode _, StartNode _ -> {
                // do nothing, skip line break
                return;
            }
        }
        builder.append("\n");
    }

    private static void divide(StringBuilder builder, Map<Node, Register> registers, Node node, Map<Integer,Integer> spillOffset) {
        Node leftNode = predecessorSkipProj(node, BinaryOperationNode.LEFT);
        Node rightNode = predecessorSkipProj(node, BinaryOperationNode.RIGHT);

        String dividend = regAllocate(leftNode, registers, spillOffset);
        String divisor = regAllocate(rightNode, registers, spillOffset);
        String dest = regAllocate(node, registers, spillOffset);

        builder.append("\tmovl ").append(dividend).append(", %eax\n");
        builder.append("\tcdq\n");
        builder.append("\tidivl ").append(divisor).append("\n");
        builder.append("\tmovl %eax, ").append(dest).append("\n"); // result
    }

    private static void mod(StringBuilder builder, Map<Node, Register> registers, Node node, Map<Integer,Integer> spillOffset) {
        Node leftNode = predecessorSkipProj(node, BinaryOperationNode.LEFT);
        Node rightNode = predecessorSkipProj(node, BinaryOperationNode.RIGHT);

        String dividend = regAllocate(leftNode, registers, spillOffset);
        String divisor = regAllocate(rightNode, registers, spillOffset);
        String dest = regAllocate(node, registers, spillOffset);

        builder.append("\tmovl ").append(dividend).append(", %eax\n");
        builder.append("\tcdq\n"); //  sign-extend, ATnT standard
        builder.append("\tidivl ").append(divisor).append("\n");
        builder.append("\tmovl %edx, ").append(dest).append("\n"); // result, notice edx here
    }

    private static void binaryAsm(StringBuilder builder, Map<Node, Register> registers, Node node, String operation, Map<Integer,Integer> spillOffset) {
        Node leftNode = predecessorSkipProj(node, BinaryOperationNode.LEFT);
        Node rightNode = predecessorSkipProj(node, BinaryOperationNode.RIGHT);

        String lhs = regAllocate(leftNode, registers, spillOffset);
        String rhs = regAllocate(rightNode, registers, spillOffset);
        String dest = regAllocate(node, registers, spillOffset);

        if (!dest.equals(lhs)) {
            builder.append("\tmovl ")
                    .append(lhs)
                    .append(", ")
                    .append(dest)
                    .append("\n");
        }

        builder.append("\t")
                .append(operation)
                .append(" ")
                .append(rhs)
                .append(", ")
                .append(dest)
                .append("\n");
    }

    private static String regAllocate(Node node, Map<Node, Register> registers, Map<Integer,Integer> spillOffset) {
        Register r = registers.get(node);
        int id = ((VirtualRegister) r).id();
        if (id < PHYS_REG_COUNT) {
            return PHYS_REGS[id];
        } else {
            int off = spillOffset.get(id);
            return "-" + off + "(%ebp)";
        }
    }


    private static void binary(StringBuilder builder, Map<Node, Register> registers, BinaryOperationNode node, String opcode
    ) {
        builder.repeat(" ", 2).append(registers.get(node))
            .append(" = ")
            .append(opcode)
            .append(" ")
            .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)))
            .append(" ")
            .append(registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)));
    }
}
