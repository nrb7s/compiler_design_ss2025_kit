package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.PhiElimination;
import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class CodeGenerator {
    private static final int PHYS_REG_COUNT = 2;
    // eax, edx are reserved for imull and divl
    private static final String[] PHYS_REGS = {
            "%ebx", "%ecx"
    };
    private static final String TEMP_REG_1 = "%esi";
    private static final String TEMP_REG_2 = "%edi";
    // Assign slot for every variable (declaration) only once
    Map<Node, Integer> slotOffset = new HashMap<>();

    public String generateAssembly(List<IrGraph> program) {
        StringBuilder builder = new StringBuilder();
        builder.append(".section .note.GNU-stack,\"\",@progbits\n\t.text\n");

        for (IrGraph graph : program) {
            PhiElimination.run(graph);
            AasmRegisterAllocator allocator = new AasmRegisterAllocator();
            Map<Node, Register> registers = allocator.allocateRegisters(graph);

            AtomicInteger nextSlot = new AtomicInteger(1);

            for (Node n : registers.keySet()) {
                if (needsSpill(n)) {
                    slotOffset.computeIfAbsent(n, __ -> nextSlot.getAndIncrement() * 4);
                }
            }

            int totalSpillBytes = (nextSlot.get() - 1) * 4;

            // Prologue and reserve stack for spilling
            builder.append("\t.globl ").append(graph.name()).append("\n");
            builder.append(graph.name()).append(":\n");
            builder.append("\tpush %rbp\n");  // 64-bit ABI, use rbp/rsp
            builder.append("\tmov %rsp, %rbp\n");
            if (totalSpillBytes > 0) {
                builder.append("\tsubq $")
                        .append(totalSpillBytes)
                        .append(", %rsp\n");
            }
            Block entry = graph.blocks().get(0);
            builder.append("\tjmp L").append(entry.getId()).append("\n");

            generateAssemblyForGraph(graph, builder, registers, slotOffset);

            // Epilogue
            builder.append("\tmovq %rbp, %rsp\n")
                    .append("\tpop %rbp\n")
                    .append("\tret\n\n");
        }
        return builder.toString();
    }

    // Only temporary SSA nodes
    private boolean needsSpill(Node n) {
        return !(n instanceof ProjNode
                || n instanceof StartNode
                || n instanceof Block
                || n instanceof ReturnNode
                || n instanceof PhiElimination.CopyNode);
    }

    private void generateAssemblyForGraph(IrGraph graph, StringBuilder b,
                                          Map<Node, Register> regs,
                                          Map<Node, Integer> slotOffset) {
        Set<Block> visited = new HashSet<>();
        Deque<Block> work = new ArrayDeque<>();
        work.add(graph.startBlock());
        while (!work.isEmpty()) {
            Block blk = work.remove();
            if (!visited.add(blk)) continue;
            b.append("L").append(blk.getId()).append(":\n");
            for (Node n : blk.nodes()) {
                scanAsm(n, b, regs, slotOffset, graph);
            }
            List<Node> nodes = blk.nodes();
            if (!nodes.isEmpty()) {
                Node last = nodes.get(nodes.size() - 1);
                boolean isReturn = last instanceof ReturnNode;
                boolean isCondJump = last instanceof CondJumpNode;
                if (!isReturn && !isCondJump && !blk.cfgSuccessors().isEmpty()) {
                    Block succ = blk.cfgSuccessors().get(0);
                    b.append("\tjmp L").append(succ.getId()).append("\n");
                }
            }
            for (Block succ : blk.cfgSuccessors()) {
                work.add(succ);
            }
        }
    }

    private void scanAsm(Node node, StringBuilder builder,
                         Map<Node, Register> registers,
                         Map<Node, Integer> slotOffset,
                         IrGraph graph) {
        switch (node) {
            case AddNode add -> binaryAsm(builder, registers, add, "addl", slotOffset, graph);
            case SubNode sub -> binaryAsm(builder, registers, sub, "subl", slotOffset, graph);
            case MulNode mul -> binaryAsm(builder, registers, mul, "imull", slotOffset, graph);
            case DivNode div -> divide(builder, registers, div, slotOffset, graph);
            case ModNode mod -> mod(builder, registers, mod, slotOffset, graph);
            case CmpGTNode gt -> genCmpSet(builder, registers, slotOffset, graph, gt, "setg");
            case CmpGENode ge -> genCmpSet(builder, registers, slotOffset, graph, ge, "setge");
            case CmpLTNode lt -> genCmpSet(builder, registers, slotOffset, graph, lt, "setl");
            case CmpLENode le -> genCmpSet(builder, registers, slotOffset, graph, le, "setle");
            case CmpEQNode eq -> genCmpSet(builder, registers, slotOffset, graph, eq, "sete");
            case CmpNENode ne -> genCmpSet(builder, registers, slotOffset, graph, ne, "setne");
            case AndNode and -> binaryAsm(builder, registers, and, "andl", slotOffset, graph);
            case OrNode or -> binaryAsm(builder, registers, or, "orl", slotOffset, graph);
            case XorNode xor -> binaryAsm(builder, registers, xor, "xorl", slotOffset, graph);
            case ShlNode shl -> shiftAsm(builder, registers, shl, "shll", slotOffset, graph);
            case ShrNode shr -> shiftAsm(builder, registers, shr, "sarl", slotOffset, graph);
            case BitwiseNotNode bn -> {
                String r = regAllocate(bn.operand(), registers, slotOffset, graph);
                if (r == null) {
                    throw new IllegalStateException("BitwiseNot operand missing register: " + bn.operand());
                }
                builder.append("\tnotl ").append(r).append("\n");
            }
            case LogicalNotNode ln -> {
                String src = regAllocate(ln.operand(), registers, slotOffset, graph);
                String dst = regAllocate(ln, registers, slotOffset, graph);
                if (src == null || dst == null) {
                    throw new IllegalStateException("LogicalNot missing register: " + ln);
                }
                builder.append("\tcmpl $0, ").append(src).append("\n")
                        .append("\tsete %al\n");
                if (isMemory(dst)) {
                    builder.append("\tmovzbl %al, ").append(TEMP_REG_1).append("\n")
                            .append("\tmovl ").append(TEMP_REG_1).append(", ").append(dst).append("\n");
                } else {
                    builder.append("\tmovzbl %al, ").append(dst).append("\n");
                }
            }
            case CondJumpNode cj -> {
                String condReg = regAllocate(cj.condition(), registers, slotOffset, graph);
                if (condReg == null) {
                    throw new IllegalStateException("CondJump condition missing register: " + cj.condition());
                }
                builder.append("\tcmpl $0, ").append(condReg).append("\n")
                        .append("\tjne L").append(cj.trueTarget().getId()).append("\n")
                        .append("\tjmp L").append(cj.falseTarget().getId()).append("\n");
            }
            case PhiElimination.CopyNode copy -> {
                String src = regAllocate(
                        predecessorSkipProj(copy, PhiElimination.CopyNode.SRC),
                        registers, slotOffset, graph
                );
                String dst = regAllocate(
                        predecessorSkipProj(copy, PhiElimination.CopyNode.DST),
                        registers, slotOffset, graph
                );
                if (src == null || dst == null || src.equals(dst)) {
                    return;
                }
                emitMov(builder, src, dst);
            }
            case ReturnNode r -> {
                Node res = predecessorSkipProj(r, ReturnNode.RESULT);
                String val = regAllocate(res, registers, slotOffset, graph);
                if (val == null) {
                    throw new IllegalStateException("Return value missing register: " + res);
                }
                builder.append("\tmovl ")
                        .append(val)
                        .append(", %eax\n");
                builder.append("\tmovq %rbp, %rsp\n")
                        .append("\tpop %rbp\n")
                        .append("\tret\n");
            }
            case ConstIntNode c -> {
                String dest = regAllocate(c, registers, slotOffset, graph);
                if (dest == null) {
                    throw new IllegalStateException("ConstInt missing register: " + c);
                }
                builder.append("\tmovl $").append(c.value())
                        .append(", ").append(dest)
                        .append("\n");
            }
            case Phi phi -> {
                throw new IllegalStateException("Unexpected Phi node " + phi);
            }
            case Block _, ProjNode _, StartNode _ -> { return; }
        }
    }

    // For a variable node with origin, use slotOffset; for a temp node without origin, use slotOffset
    private static String regAllocate(Node node, Map<Node, Register> registers,
                                      Map<Node, Integer> slotOffset,
                                      IrGraph graph) {
        Register r = registers.get(node);
        if (r == null) {
            // Nodes that do not require a register simply return null
            if (node instanceof ProjNode
                    || node instanceof StartNode
                    || node instanceof Block
                    || node instanceof ReturnNode
                    || node instanceof PhiElimination.CopyNode) {
                return null;
            }
            throw new IllegalStateException(
                    "Register allocator did not assign a register to node " + node
                            + " of type " + node.getClass().getSimpleName());
        }
        int id = ((VirtualRegister) r).id();
        if (id < PHYS_REG_COUNT) {
            return PHYS_REGS[id];
        } else {
            Integer off = slotOffset.get(node);
            if (off == null) {
                if (node instanceof ProjNode
                        || node instanceof StartNode
                        || node instanceof Block
                        || node instanceof ReturnNode
                        || node instanceof PhiElimination.CopyNode) {
                    return null;
                }
                throw new IllegalStateException("No register for node: " + node + " (" + node.getClass() + ")");
            }
            return "-" + off + "(%rbp)";
        }
    }

    // Helper for binary operations (add, sub, and, or, etc.)
    private static void binaryAsm(StringBuilder builder, Map<Node, Register> registers, Node node, String operation,
                                  Map<Node, Integer> slotOffset, IrGraph graph) {
        Node leftNode = predecessorSkipProj(node, BinaryOperationNode.LEFT);
        Node rightNode = predecessorSkipProj(node, BinaryOperationNode.RIGHT);

        String lhs = regAllocate(leftNode, registers, slotOffset, graph);
        String rhs = regAllocate(rightNode, registers, slotOffset, graph);
        String dest = regAllocate(node, registers, slotOffset, graph);

        if (lhs == null || rhs == null || dest == null) {
            throw new IllegalStateException("Binary op missing register: " + node);
        }

        if (!dest.equals(lhs)) {
            emitMov(builder, lhs, dest);
        }

        if (operation.equals("imull")) {
            if (isMemory(rhs) || isMemory(dest)) {
                builder.append("\tmovl ").append(dest).append(", ").append(TEMP_REG_1).append("\n");
                if (isMemory(rhs)) {
                    builder.append("\tmovl ").append(rhs).append(", ").append(TEMP_REG_2).append("\n");
                    builder.append("\timull ").append(TEMP_REG_2).append(", ").append(TEMP_REG_1).append("\n");
                } else {
                    builder.append("\timull ").append(rhs).append(", ").append(TEMP_REG_1).append("\n");
                }
                builder.append("\tmovl ").append(TEMP_REG_1).append(", ").append(dest).append("\n");
            } else {
                builder.append("\timull ").append(rhs).append(", ").append(dest).append("\n");
            }
        } else {
            if (isMemory(rhs) && isMemory(dest)) {
                builder.append("\tmovl ").append(rhs).append(", ").append(TEMP_REG_1).append("\n");
                builder.append("\t").append(operation).append(" ").append(TEMP_REG_1).append(", ").append(dest).append("\n");
            } else {
                builder.append("\t").append(operation).append(" ").append(rhs).append(", ").append(dest).append("\n");
            }
        }
    }

    // For division
    private static void divide(StringBuilder builder, Map<Node, Register> registers, Node node,
                               Map<Node, Integer> slotOffset, IrGraph graph) {
        Node leftNode = predecessorSkipProj(node, BinaryOperationNode.LEFT);
        Node rightNode = predecessorSkipProj(node, BinaryOperationNode.RIGHT);

        String dividend = regAllocate(leftNode, registers, slotOffset, graph);
        String rawDivisor = regAllocate(rightNode, registers, slotOffset, graph);
        String dest = regAllocate(node, registers, slotOffset, graph);
        if (dividend == null || rawDivisor == null || dest == null) {
            throw new IllegalStateException("Divide missing register: " + node);
        }
        String divisor = rawDivisor;

        // Avoid mem/mem
        if (isMemory(rawDivisor) || rawDivisor.equals("%edx") || rawDivisor.equals("%eax")) {
            builder.append("\tmovl ").append(rawDivisor).append(", ").append(TEMP_REG_1).append("\n");
            divisor = TEMP_REG_1;
        }

        builder.append("\tmovl ").append(dividend).append(", %eax\n");
        builder.append("\tcdq\n");
        builder.append("\tidivl ").append(divisor).append("\n");
        builder.append("\tmovl %eax, ").append(dest).append("\n");
    }

    // For modulus
    private static void mod(StringBuilder builder, Map<Node, Register> registers, Node node,
                            Map<Node, Integer> slotOffset, IrGraph graph) {
        Node leftNode = predecessorSkipProj(node, BinaryOperationNode.LEFT);
        Node rightNode = predecessorSkipProj(node, BinaryOperationNode.RIGHT);

        String dividend = regAllocate(leftNode, registers, slotOffset, graph);
        String rawDivisor = regAllocate(rightNode, registers, slotOffset, graph);
        String dest = regAllocate(node, registers, slotOffset, graph);
        if (dividend == null || rawDivisor == null || dest == null) {
            throw new IllegalStateException("Modulus missing register: " + node);
        }
        String divisor = rawDivisor;

        if (isMemory(rawDivisor) || rawDivisor.equals("%edx") || rawDivisor.equals("%eax")) {
            builder.append("\tmovl ").append(rawDivisor).append(", ").append(TEMP_REG_1).append("\n");
            divisor = TEMP_REG_1;
        }

        builder.append("\tmovl ").append(dividend).append(", %eax\n");
        builder.append("\tcdq\n");
        builder.append("\tidivl ").append(divisor).append("\n");
        builder.append("\tmovl %edx, ").append(dest).append("\n");
    }

    // For comparisons
    private static void genCmpSet(StringBuilder b, Map<Node, Register> regs,
                                  Map<Node, Integer> slotOffset, IrGraph graph,
                                  BinaryOperationNode cmp, String setInstr) {

        String lhs = regAllocate(predecessorSkipProj(cmp, BinaryOperationNode.LEFT), regs, slotOffset, graph);
        String rhs = regAllocate(predecessorSkipProj(cmp, BinaryOperationNode.RIGHT), regs, slotOffset, graph);
        String dst = regAllocate(cmp, regs, slotOffset, graph);

        if (lhs == null || rhs == null || dst == null) {
            throw new IllegalStateException("Comparison missing register: " + cmp);
        }

        if (isMemory(lhs) && isMemory(rhs)) {
            b.append("\tmovl ").append(rhs).append(", %esi\n");
            b.append("\tcmpl %esi, ").append(lhs).append("\n");
        } else {
            b.append("\tcmpl ").append(rhs).append(", ").append(lhs).append("\n");
        }

        b.append("\t").append(setInstr).append(" %al\n");

        if (isMemory(dst)) {
            b.append("\tmovzbl %al, ").append(TEMP_REG_1).append("\n")
                    .append("\tmovl ").append(TEMP_REG_1).append(", ").append(dst).append("\n");
        } else {
            b.append("\tmovzbl %al, ").append(dst).append("\n");
        }
    }

    // For shifts
    private static void shiftAsm(StringBuilder b, Map<Node, Register> regs,
                                 BinaryOperationNode sh, String instr,
                                 Map<Node, Integer> slotOffset, IrGraph graph) {

        Node srcNode = predecessorSkipProj(sh, BinaryOperationNode.LEFT);
        Node amtNode = predecessorSkipProj(sh, BinaryOperationNode.RIGHT);

        String src = regAllocate(srcNode, regs, slotOffset, graph);
        String amt = regAllocate(amtNode, regs, slotOffset, graph);
        String dst = regAllocate(sh, regs, slotOffset, graph);

        if (src == null || amt == null || dst == null) {
            throw new IllegalStateException("Shift missing register: " + sh);
        }

        if (!dst.equals(src)) {
            emitMov(b, src, dst);
        }
        if (isMemory(amt) || !amt.equals("%ecx")) {
            b.append("\tmovl ").append(amt).append(", %ecx\n");
        }

        b.append("\tandl $0x1F, %ecx\n");
        b.append("\t").append(instr).append(" %cl, ").append(dst).append("\n");
    }

    // Helper to detect whether a string represents a memory location
    private static boolean isMemory(String s) {
        return s.contains("(%");
    }

    // Emit a mov instruction while avoiding memory-to-memory transfers
    private static void emitMov(StringBuilder b, String src, String dst) {
        if (isMemory(src) && isMemory(dst)) {
            b.append("\tmovl ").append(src).append(", ")
                    .append(TEMP_REG_1).append("\n");
            b.append("\tmovl ").append(TEMP_REG_1)
                    .append(", ").append(dst).append("\n");
        } else {
            b.append("\tmovl ").append(src).append(", ").append(dst).append("\n");
        }
    }
}
