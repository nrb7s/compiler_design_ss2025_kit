package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.*;

import java.util.*;

import static edu.kit.kastel.vads.compiler.ir.util.NodeSupport.predecessorSkipProj;

 // Φ -> Copy
public final class PhiElimination {

    private PhiElimination() {}

    public static void run(IrGraph g) {
        List<Block> rpo = reversePostOrder(g);

        for (Block b : rpo) {
            ListIterator<Node> it = b.nodes().listIterator();
            while (it.hasNext()) {
                Node n = it.next();
                if (!(n instanceof Phi phi)) continue;
                Node dst = phi;
                int idx = 0;
                for (Node predNode : b.predecessors()) {
                    Block pred = predNode.block();
                    Node src = predecessorSkipProj(phi, idx++);

                    if (src == dst) continue;

                    CopyNode cp = new CopyNode(pred, src, dst);
                    insertBeforeTerminator(pred, cp);
                }
                it.remove();
            }
        }
    }

     private static List<Block> reversePostOrder(IrGraph g) {
         List<Block> order = new ArrayList<>();
         Set<Block> seen = new HashSet<>();
         dfs(g.endBlock(), seen, order);
         Collections.reverse(order);
         return order;
     }

     private static void dfs(Block b, Set<Block> seen, List<Block> order) {
         if (!seen.add(b)) return;
         for (Node predTerm : b.predecessors())
             dfs(predTerm.block(), seen, order);
         order.add(b);
     }

     private static void insertBeforeTerminator(Block blk, Node copy) {
         List<Node> ns = blk.nodes();
         if (ns.isEmpty()) { ns.add(copy); return; }
         Node last = ns.getLast();
         if (last instanceof CondJumpNode || last instanceof ReturnNode) {
             ns.add(ns.size() - 1, copy);
         } else {
             ns.add(copy);
         }
     }

     public static final class CopyNode extends Node {
         public static final int SRC = 0, DST = 1;
         public CopyNode(Block b, Node src, Node dst) { super(b, src, dst); }
         public Node src() { return predecessor(SRC); }
         public Node dst() { return predecessor(DST); }
         @Override protected String info() { return src() + "→" + dst(); }
     }
}
