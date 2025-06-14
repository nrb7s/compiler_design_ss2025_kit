package edu.kit.kastel.vads.compiler.backend.aasm;

import edu.kit.kastel.vads.compiler.backend.regalloc.Register;
import edu.kit.kastel.vads.compiler.ir.IrGraph;
import edu.kit.kastel.vads.compiler.ir.PhiElimination;
import edu.kit.kastel.vads.compiler.ir.node.*;

import java.util.*;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

public class CodeGenerator {
    private static final int PHYS_REG_COUNT = 2;
    // eax, edx reserved for imull and divl
    private static final String[] PHYS_REGS = {
            "%ebx", "%ecx"
    };
    private static final String TEMP_REG_1 = "%esi";
    private static final String TEMP_REG_2 = "%edi";

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
            builder.append("\tpush %rbp\n");  // notice 64bit here
            builder.append("\tmov %rsp, %rbp\n");
            if (totalSpillBytes > 0) {
                builder.append("\tsubl $")
                        .append(totalSpillBytes)
                        .append(", %esp\n");
            }
            List<Block> blocks = graph.blocks();
            Block entry = blocks.get(0);
            builder.append("\tjmp L").append(entry.getId()).append("\n");

            generateAssemblyForGraph(graph, builder, registers, spillOffset);

            // Epilogue
            // builder.append("\tpopp %rbp\n");
            // builder.append("\tleave\n"); // leave = movl %ebp, %esp then popl %ebp
            builder.append("\tmov %rbp, %rsp\n")
                    .append("\tpop %rbp\n");
            builder.append("\tret\n\n");
        }
        return builder.toString();
    }

    private void generateAssemblyForGraph(IrGraph graph,
                                          StringBuilder b,
                                          Map<Node,Register> regs,
                                          Map<Integer,Integer> spill) {
        Set<Block> visited = new HashSet<>();
        Deque<Block> work  = new ArrayDeque<>();
        work.add(graph.startBlock());

        // debug
        /*
        System.out.println("IR blocks: ");
        for (Block blk : graph.blocks()) {
            System.out.println("Block L" + blk.getId() + ":");
            for (Node n : blk.nodes()) {
                System.out.println("   " + n + " (" + n.getClass().getSimpleName() + ")");
            }
        }
         */
        // ends

        while (!work.isEmpty()) {
            Block blk = work.remove();
            if (!visited.add(blk)) continue;

            // emit the label
            b.append("L").append(blk.getId()).append(":\n");

            // emit every IR-node in that block
            for (Node n : blk.nodes()) {
                scanAsm(n, b, regs, spill);
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

            // now follow _cfg_ successors, not dataflow successors of the node
            for (Block succ : blk.cfgSuccessors()) {
                work.add(succ);
            }
        }
    }

    private void scanAsm(Node node, StringBuilder builder, Map<Node, Register> registers, Map<Integer,Integer> spillOffset) {

        switch (node) {
            case AddNode add -> binaryAsm(builder, registers, add, "addl", spillOffset);
            case SubNode sub -> binaryAsm(builder, registers, sub, "subl", spillOffset);
            case MulNode mul -> binaryAsm(builder, registers, mul, "imull", spillOffset);  // 32-bit, imulq for 64-bit
            case DivNode div -> divide(builder, registers, div, spillOffset);
            case ModNode mod -> mod(builder, registers, mod, spillOffset);
            // L2
            case CmpGTNode gt -> genCmpSet(builder, registers, spillOffset, gt, "setg");
            case CmpGENode ge -> genCmpSet(builder, registers, spillOffset, ge, "setge");
            case CmpLTNode lt -> genCmpSet(builder, registers, spillOffset, lt, "setl");
            case CmpLENode le -> genCmpSet(builder, registers, spillOffset, le, "setle");
            case CmpEQNode eq -> genCmpSet(builder, registers, spillOffset, eq, "sete");
            case CmpNENode ne -> genCmpSet(builder, registers, spillOffset, ne, "setne");

            case AndNode and -> binaryAsm(builder, registers, and, "andl", spillOffset);
            case OrNode  or  -> binaryAsm(builder, registers, or,  "orl" , spillOffset);
            case XorNode xor -> binaryAsm(builder, registers, xor, "xorl", spillOffset);

            case ShlNode shl -> shiftAsm(builder, registers, shl, "shll", spillOffset); // <<
            case ShrNode shr -> shiftAsm(builder, registers, shr, "sarl", spillOffset); // >>

            case BitwiseNotNode bn -> {
                String r = regAllocate(bn.operand(), registers, spillOffset);
                builder.append("\tnotl ").append(r).append("\n");
            }
            case LogicalNotNode ln -> {
                String src = regAllocate(ln.operand(), registers, spillOffset);
                String dst = regAllocate(ln, registers, spillOffset);
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
                String condReg = regAllocate(cj.condition(), registers, spillOffset);
                builder.append("\tcmpl $0, ").append(condReg).append("\n")
                        .append("\tjne L").append(cj.trueTarget().getId()).append("\n")
                        .append("\tjmp L").append(cj.falseTarget().getId()).append("\n");
            }
            case PhiElimination.CopyNode copy -> {
                String src = regAllocate(
                        predecessorSkipProj(copy, PhiElimination.CopyNode.SRC),
                        registers, spillOffset
                );
                String dst = regAllocate(
                        predecessorSkipProj(copy, PhiElimination.CopyNode.DST),
                        registers, spillOffset
                );
                if (!src.equals(dst)) {
                    builder.append("\tmovl ").append(src).append(", ").append(dst).append("\n");
                }
            }
            // L2 ends
            case ReturnNode r -> {
                Node res = predecessorSkipProj(r, ReturnNode.RESULT);
                builder.append("\tmovl ")
                        .append(regAllocate(res, registers, spillOffset))
                        .append(", %eax\n");
                builder.append("\tmov %rbp, %rsp\n")
                        .append("\tpop %rbp\n")
                        .append("\tret\n");
                return;
            }
            case ConstIntNode c -> {
                String dest = regAllocate(c, registers, spillOffset);
                builder.append("\tmovl $").append(c.value())
                        .append(", ").append(dest)
                        .append("\n");
            }
            case Phi _ -> {
                return;
                // throw new UnsupportedOperationException("phi");
            }
            case Block _, ProjNode _, StartNode _ -> {
                // do nothing, skip line break
                return;
            }
        }
    }

    /*
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
     */

    private static void divide(StringBuilder builder, Map<Node, Register> registers, Node node, Map<Integer,Integer> spillOffset) {
        Node leftNode = predecessorSkipProj(node, BinaryOperationNode.LEFT);
        Node rightNode = predecessorSkipProj(node, BinaryOperationNode.RIGHT);

        String dividend = regAllocate(leftNode, registers, spillOffset);
        String rawDivisor = regAllocate(rightNode, registers, spillOffset);
        String dest = regAllocate(node, registers, spillOffset);
        String divisor = rawDivisor;

        // is mem to mem or edx will collapse with cdq since cdq -> eax -> edx:eax, clear for both
        if (isMemory(rawDivisor) || rawDivisor.equals("%edx") || rawDivisor.equals("%eax")) {
            builder.append("\tmovl ").append(rawDivisor).append(", ").append(TEMP_REG_1).append("\n");
            divisor = TEMP_REG_1;
        }

        builder.append("\tmovl ").append(dividend).append(", %eax\n");
        builder.append("\tcdq\n");
        builder.append("\tidivl ").append(divisor).append("\n");
        builder.append("\tmovl %eax, ").append(dest).append("\n"); // result
    }

    private static void mod(StringBuilder builder, Map<Node, Register> registers, Node node, Map<Integer,Integer> spillOffset) {
        Node leftNode = predecessorSkipProj(node, BinaryOperationNode.LEFT);
        Node rightNode = predecessorSkipProj(node, BinaryOperationNode.RIGHT);

        String dividend = regAllocate(leftNode, registers, spillOffset);
        String rawDivisor = regAllocate(rightNode, registers, spillOffset);
        String dest = regAllocate(node, registers, spillOffset);
        String divisor = rawDivisor;

        if (isMemory(rawDivisor) || rawDivisor.equals("%edx") || rawDivisor.equals("%eax")) {
            builder.append("\tmovl ").append(rawDivisor).append(", ").append(TEMP_REG_1).append("\n");
            divisor = TEMP_REG_1;
        }

        builder.append("\tmovl ").append(dividend).append(", %eax\n");
        builder.append("\tcdq\n"); //  sign-extend, ATnT standard
        builder.append("\tidivl ").append(divisor).append("\n");
        builder.append("\tmovl %edx, ").append(dest).append("\n"); // result, notice edx here (cuz its mod
    }

    private static void binaryAsm(StringBuilder builder, Map<Node, Register> registers, Node node, String operation, Map<Integer,Integer> spillOffset) {
        Node leftNode = predecessorSkipProj(node, BinaryOperationNode.LEFT);
        Node rightNode = predecessorSkipProj(node, BinaryOperationNode.RIGHT);

        String lhs = regAllocate(leftNode, registers, spillOffset);
        String rhs = regAllocate(rightNode, registers, spillOffset);
        String dest = regAllocate(node, registers, spillOffset);

        if (!dest.equals(lhs)) {
            if (isMemory(lhs) && isMemory(dest)) {
                // mem to mem move, use eax as medium
                builder.append("\tmovl ").append(lhs).append(",").append(TEMP_REG_1).append("\n");
                builder.append("\tmovl ").append(TEMP_REG_1).append(", ").append(dest).append("\n");
            } else {
                builder.append("\tmovl ").append(lhs).append(", ").append(dest).append("\n");
            }
        }

        if (operation.equals("imull")){ // imull requires 2 regs
            if (isMemory(rhs) || isMemory(dest)){
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
            if (isMemory(rhs) && isMemory(dest)) { // eax as medium to avoid mem to mem
                builder.append("\tmovl ").append(rhs).append(", ").append(TEMP_REG_1).append("\n");
                builder.append("\t").append(operation).append(" ").append(TEMP_REG_1).append(", ").append(dest).append("\n");
            } else {
                builder.append("\t").append(operation).append(" ").append(rhs).append(", ").append(dest).append("\n");
            }
        }
    }

    private static String regAllocate(Node node, Map<Node, Register> registers, Map<Integer,Integer> spillOffset) {
        Register r = registers.get(node);
        if (r == null) {
            System.err.println("No register for node: " + node + " (" + node.getClass() + ")");
        }
        int id = ((VirtualRegister) r).id();
        if (id < PHYS_REG_COUNT) {
            return PHYS_REGS[id];
        } else {
            int off = spillOffset.get(id);
            return "-" + off + "(%rbp)";
        }
    }

    // L2
    private void genCmpSet(StringBuilder b, Map<Node,Register> regs,
                           Map<Integer,Integer> spill,
                           BinaryOperationNode cmp, String setInstr) {

        String lhs = regAllocate(predecessorSkipProj(cmp, BinaryOperationNode.LEFT),  regs, spill);
        String rhs = regAllocate(predecessorSkipProj(cmp, BinaryOperationNode.RIGHT), regs, spill);
        String dst = regAllocate(cmp, regs, spill);

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

    private void shiftAsm(StringBuilder b, Map<Node,Register> regs,
                          BinaryOperationNode sh, String instr, Map<Integer,Integer> spill) {

        Node srcNode = predecessorSkipProj(sh, BinaryOperationNode.LEFT);
        Node amtNode = predecessorSkipProj(sh, BinaryOperationNode.RIGHT);

        String src = regAllocate(srcNode, regs, spill);
        String amt = regAllocate(amtNode, regs, spill);
        String dst = regAllocate(sh, regs, spill);

        // mov src -> dst
        if (!dst.equals(src))
            b.append("\tmovl ").append(src).append(", ").append(dst).append("\n");
        // offset in cl
        if (isMemory(amt) || !amt.equals("%ecx")) {
            b.append("\tmovl ").append(amt).append(", %ecx\n");
        }

        b.append("\tandl $0x1F, %ecx\n");
        b.append("\t").append(instr).append(" %cl, ").append(dst).append("\n");
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

    private static boolean isMemory(String s){
        return s.contains("(%");
    }
}
