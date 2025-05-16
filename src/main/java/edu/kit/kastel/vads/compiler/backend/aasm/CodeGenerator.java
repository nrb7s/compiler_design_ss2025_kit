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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class CodeGenerator {

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
        for (IrGraph graph : program) {
            AasmRegisterAllocator allocator = new AasmRegisterAllocator();
            Map<Node, Register> registers = allocator.allocateRegisters(graph);
            builder.append("\t.text\n")
                    .append("\t.globl ")
                    .append(graph.name())
                    .append("\n")
                    .append(graph.name())
                    .append(":\n");

            // Prologue, though not needed for simple compiling like this
            builder.append("\tpushl %ebp\n");
            builder.append("\tmovl %esp, %ebp\n");

            generateAssemblyForGraph(graph, builder, registers);

            // Epilogue
            builder.append("\tpopl %ebp\n");
            builder.append("\tret\n");
        }
        return builder.toString();
    }

    private void generateAssemblyForGraph(IrGraph graph, StringBuilder builder, Map<Node, Register> registers) {
        Set<Node> visited = new HashSet<>();
        scanAsm(graph.endBlock(), visited, builder, registers);
    }

    private void scanAsm(Node node, Set<Node> visited, StringBuilder builder, Map<Node, Register> registers) {
        for (Node predecessor : node.predecessors()) {
            if (!visited.contains(predecessor)) {
                scanAsm(predecessor, visited, builder, registers);
            }
        }

        switch (node) {
            case AddNode add -> binaryAsm(builder, registers, add, "addq");
            case SubNode sub -> binaryAsm(builder, registers, sub, "subq");
            case MulNode mul -> binaryAsm(builder, registers, mul, "imulq");  // 32-bit, imulq for 64-bit
            case DivNode div -> divide(builder, registers, div);
            case ModNode mod -> mod(builder, registers, mod);
            case ReturnNode r -> {
                Node res = predecessorSkipProj(r, ReturnNode.RESULT);
                builder.append("\tmovq ")
                        .append(regAllocate(registers.get(res)))
                        .append(", %eax\n");
            }
            case ConstIntNode c -> {
                String dest = regAllocate(registers.get(c));
                builder.append("\tmovq $").append(c.value())
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

    private static void divide(StringBuilder builder, Map<Node, Register> registers, Node node) {
        String dividend = regAllocate(registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)));
        String divisor = regAllocate(registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)));
        String dest = regAllocate(registers.get(node));

        builder.append("\tmovq ").append(dividend).append(", %eax\n");
        builder.append("\tcqto\n");
        builder.append("\tidivq ").append(divisor).append("\n");
        builder.append("\tmovq %eax, ").append(dest).append("\n"); // result
    }

    private static void mod(StringBuilder builder, Map<Node, Register> registers, Node node) {
        String dividend = regAllocate(registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)));
        String divisor = regAllocate(registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)));
        String dest = regAllocate(registers.get(node));

        builder.append("\tmovq ").append(dividend).append(", %eax\n");
        builder.append("\tcqto\n"); //  sign-extend, ATnT standard
        builder.append("\tidivq ").append(divisor).append("\n");
        builder.append("\tmovq %edx, ").append(dest).append("\n"); // result, notice edx here
    }

    private static void binaryAsm(StringBuilder builder, Map<Node, Register> registers, Node node, String operation) {
        String lhs = regAllocate(registers.get(predecessorSkipProj(node, BinaryOperationNode.LEFT)));
        String rhs = regAllocate(registers.get(predecessorSkipProj(node, BinaryOperationNode.RIGHT)));
        String dest = regAllocate(registers.get(node));

        if (!dest.equals(lhs)) {
            builder.append("\tmovq ")
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

    private static String regAllocate(Register r) {
        int id = ((VirtualRegister) r).id();
        return switch (id) {
            /*
            case 0 -> "%r8d";
            case 1 -> "%r9d";
            case 2 -> "%r10d";
            case 3 -> "%r11d";
            case 4 -> "%r12d";
            case 5 -> "%r13d";
            case 6 -> "%r14d";
            case 7 -> "%r15d";
             */
            case 0 -> "%rax";
            case 1 -> "%rbx";
            case 2 -> "%rcx";
            case 3 -> "%rdx";
            case 4 -> "%rsi";
            case 5 -> "%rdi";
            default -> throw new IllegalArgumentException("Too many registers: " + id);
        };
    }

    /*
    private static String getOperand(Node node, Map<Node, Register> registers) {
        return node instanceof ConstIntNode c
                ? "$" + c.value() // use immediate constant
                : regAllocate(registers.get(node)); // fallback to register
    }
     */


    private static void binary(
        StringBuilder builder,
        Map<Node, Register> registers,
        BinaryOperationNode node,
        String opcode
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
