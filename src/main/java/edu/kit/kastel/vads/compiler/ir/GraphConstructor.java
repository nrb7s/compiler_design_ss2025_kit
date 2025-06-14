package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class GraphConstructor {

    private final Optimizer optimizer;
    private final IrGraph graph;
    private final Map<Name, Map<Block, Node>> currentDef = new HashMap<>();
    private final Map<Block, Map<Name, Phi>> incompletePhis = new HashMap<>();
    private final Map<Block, Node> currentSideEffect = new HashMap<>();
    private final Map<Block, Phi> incompleteSideEffectPhis = new HashMap<>();
    private final Set<Block> sealedBlocks = new HashSet<>();
    private final Deque<Block> breakTargetStack = new ArrayDeque<>();
    private final Deque<Block> continueTargetStack = new ArrayDeque<>();
    private @Nullable Block currentBlock;

    public GraphConstructor(Optimizer optimizer, String name) {
        this.optimizer = optimizer;
        this.graph = new IrGraph(name);
        this.currentBlock = this.graph.startBlock();
        // the start block never gets any more predecessors
        sealBlock(this.currentBlock);
    }

    public Node newStart() {
        assert currentBlock() == this.graph.startBlock() : "start must be in start block";
        return new StartNode(currentBlock());
    }

    public Node newAdd(Node left, Node right) {
        AddNode n = new AddNode(currentBlock(), left, right);
        currentBlock().addNode(n);
        return optimizer.transform(n);
    }

    public Node newSub(Node left, Node right) {
        SubNode n = new SubNode(currentBlock(), left, right);
        currentBlock().addNode(n);
        return optimizer.transform(n);
    }

    public Node newMul(Node left, Node right) {
        MulNode n = new MulNode(currentBlock(), left, right);
        currentBlock().addNode(n);
        return optimizer.transform(n);
    }

    public Node newDiv(Node left, Node right) {
        DivNode n = new DivNode(currentBlock(), left, right,
                readCurrentSideEffect());
        currentBlock().addNode(n);
        return optimizer.transform(n);
    }

    public Node newMod(Node left, Node right) {
        ModNode n = new ModNode(currentBlock(), left, right,
                readCurrentSideEffect());
        currentBlock().addNode(n);
        return optimizer.transform(n);
    }

    public Node newReturn(Node result) {
        return new ReturnNode(currentBlock(), readCurrentSideEffect(), result);
    }

    public Node newConstInt(int value) {
        // always move const into start block, this allows better deduplication
        // and resultingly in better value numbering
        ConstIntNode c = new ConstIntNode(this.graph.startBlock(), value);
        this.graph.startBlock().addNode(c);
        return this.optimizer.transform(c);
    }

    public Node newSideEffectProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.SIDE_EFFECT);
    }

    public Node newResultProj(Node node) {
        return new ProjNode(currentBlock(), node, ProjNode.SimpleProjectionInfo.RESULT);
    }

    public Block currentBlock() {
        return this.currentBlock;
    }

    public Phi newPhi() {
        // don't transform phi directly, it is not ready yet
        return new Phi(currentBlock());
    }

    // L2
    public void setCurrentBlock(Block block) {
        this.currentBlock = block;
    }

    public Node newCmpGT(Node l, Node r) {  // >
        return optimizer.transform(new CmpGTNode(currentBlock(), l, r));
    }

    public Node newCmpGE(Node l, Node r) {  // >=
        return optimizer.transform(new CmpGENode(currentBlock(), l, r));
    }

    public Node newCmpLT(Node l, Node r) {  // <
        return optimizer.transform(new CmpLTNode(currentBlock(), l, r));
    }

    public Node newCmpLE(Node l, Node r) {  // <=
        return optimizer.transform(new CmpLENode(currentBlock(), l, r));
    }

    public Node newCmpEQ(Node l, Node r) {  // ==
        return optimizer.transform(new CmpEQNode(currentBlock(), l, r));
    }

    public Node newCmpNE(Node l, Node r) {  // !=
        return optimizer.transform(new CmpNENode(currentBlock(), l, r));
    }

    public Node newAnd(Node l, Node r) {     // &
        return optimizer.transform(new AndNode(currentBlock(), l, r));
    }

    public Node newOr(Node l, Node r) {      // |
        return optimizer.transform(new OrNode(currentBlock(), l, r));
    }

    public Node newXor(Node l, Node r) {     // ^
        return optimizer.transform(new XorNode(currentBlock(), l, r));
    }

    // r & 0x1F
    public Node newShl(Node l, Node r) {     // <<
        return optimizer.transform(new ShlNode(currentBlock(), l, r));
    }

    public Node newShr(Node l, Node r) {     // >>
        return optimizer.transform(new ShrNode(currentBlock(), l, r));
    }


    public Node newBitwiseNot(Node operand) {
        BitwiseNotNode node = new BitwiseNotNode(currentBlock(), operand);
        currentBlock().addNode(node);
        return this.optimizer.transform(node);
    }

    public Node newLogicalNot(Node operand) {
        LogicalNotNode node = new LogicalNotNode(currentBlock(), operand);
        currentBlock().addNode(node);
        return this.optimizer.transform(node);
    }

    public IrGraph graph() {
        return this.graph;
    }

    void writeVariable(Name variable, Block block, Node value) {
        this.currentDef.computeIfAbsent(variable, _ -> new HashMap<>()).put(block, value);
    }

    Node readVariable(Name variable, Block block) {
        Node node = this.currentDef.getOrDefault(variable, Map.of()).get(block);
        if (node != null) {
            return node;
        }
        return readVariableRecursive(variable, block);
    }


    private Node readVariableRecursive(Name variable, Block block) {
        Node val;
        if (!this.sealedBlocks.contains(block)) {
            val = newPhi();
            this.incompletePhis.computeIfAbsent(block, _ -> new HashMap<>()).put(variable, (Phi) val);
        } else if (block.predecessors().size() == 1) {
            val = readVariable(variable, block.predecessors().getFirst().block());
        } else {
            val = newPhi();
            writeVariable(variable, block, val);
            val = addPhiOperands(variable, (Phi) val);
        }
        writeVariable(variable, block, val);
        return val;
    }

    Node addPhiOperands(Name variable, Phi phi) {
        for (Node pred : phi.block().predecessors()) {
            phi.appendOperand(readVariable(variable, pred.block()));
        }
        phi.block().nodes().add(0, phi);
        return tryRemoveTrivialPhi(phi);
    }

    Node tryRemoveTrivialPhi(Phi phi) {
        Node same = null;
        for (Node op : phi.predecessors()) {
            if (op == phi) continue;
            if (same == null) {
                same = op;
            } else if (same != op) {
                return phi;
            }
        }

        if (same == null) {
            return phi;
        }

        // Remove the phi from its block, if it was inserted
        phi.block().nodes().remove(phi);

        // Redirect all uses to the single operand
        for (Node succ : phi.graph().successors(phi)) {
            List<? extends Node> preds = succ.predecessors();
            for (int i = 0; i < preds.size(); i++) {
                if (succ.predecessor(i) == phi) {
                    succ.setPredecessor(i, same);
                }
            }
        }

        return same;
    }

    void sealBlock(Block block) {
        for (Map.Entry<Name, Phi> entry : this.incompletePhis.getOrDefault(block, Map.of()).entrySet()) {
            addPhiOperands(entry.getKey(), entry.getValue());
        }
        this.sealedBlocks.add(block);
    }

    public void writeCurrentSideEffect(Node node) {
        writeSideEffect(currentBlock(), node);
    }

    private void writeSideEffect(Block block, Node node) {
        this.currentSideEffect.put(block, node);
    }

    public Node readCurrentSideEffect() {
        return readSideEffect(currentBlock());
    }

    private Node readSideEffect(Block block) {
        Node node = this.currentSideEffect.get(block);
        if (node != null) {
            return node;
        }
        return readSideEffectRecursive(block);
    }

    private Node readSideEffectRecursive(Block block) {
        Node val;
        if (!this.sealedBlocks.contains(block)) {
            val = newPhi();
            Phi old = this.incompleteSideEffectPhis.put(block, (Phi) val);
            assert old == null : "double readSideEffectRecursive for " + block;
        } else if (block.predecessors().size() == 1) {
            val = readSideEffect(block.predecessors().getFirst().block());
        } else {
            val = newPhi();
            writeSideEffect(block, val);
            val = addPhiOperands((Phi) val);
        }
        writeSideEffect(block, val);
        return val;
    }

    Node addPhiOperands(Phi phi) {
        for (Node pred : phi.block().predecessors()) {
            phi.appendOperand(readSideEffect(pred.block()));
        }
        phi.block().nodes().add(0, phi);
        return tryRemoveTrivialPhi(phi);
    }

    // L2: AST -> IR
    public static IrGraph build(FunctionTree function) {
        // debug
        // System.out.println("DEBUG: building IR for " + function.name() + ", body=" + function.body());
        Optimizer optimizer = new Optimizer() {
            @Override
            public Node transform(Node n) { return n; }
        };
        GraphConstructor gc = new GraphConstructor(optimizer, function.name().name().asString());
        gc.buildBody(function.body());
        return gc.graph();
    }

    private void buildBody(StatementTree stmt) {
        // System.out.println("DEBUG: buildBody got " + stmt.getClass() + " " + stmt);
        if (stmt instanceof BlockTree block) {
            // System.out.println("DEBUG: BlockTree with " + block.statements().size() + " statements");
            for (StatementTree s : block.statements()) {
                if (currentBlock == null) { break;}
                buildBody(s);
            }
            return;
        } else if (stmt instanceof DeclarationTree decl) {
            if (decl.initializer() != null) {
                Node initVal = buildExpr(decl.initializer());
                Name varName = decl.name().name();
                writeVariable(varName, currentBlock(), initVal);
            }
        } else if (stmt instanceof AssignmentTree assign) {
            Node value = buildExpr(assign.expression());
            Name var = extractName(assign.lValue());
            // +=，-=，*=，/=，%=
            switch (assign.operator().type()) {
                case ASSIGN -> writeVariable(var, currentBlock(), value);
                case ASSIGN_PLUS -> writeVariable(var, currentBlock(),
                        newAdd(readVariable(var, currentBlock()), value));
                case ASSIGN_MINUS -> writeVariable(var, currentBlock(),
                        newSub(readVariable(var, currentBlock()), value));
                case ASSIGN_MUL -> writeVariable(var, currentBlock(),
                        newMul(readVariable(var, currentBlock()), value));
                case ASSIGN_DIV -> writeVariable(var, currentBlock(),
                        newDiv(readVariable(var, currentBlock()), value));
                case ASSIGN_MOD -> writeVariable(var, currentBlock(),
                        newMod(readVariable(var, currentBlock()), value));
                default -> throw new UnsupportedOperationException("Unsupported assignment operator");
            }
        } else if (stmt instanceof IfTree ifTree) {
            buildIf(ifTree);
        } else if (stmt instanceof WhileLoopTree whileTree) {
            // debug
            // System.out.println("DEBUG: building while, cond=" + whileTree.condition());
            buildWhile(whileTree);
        } else if (stmt instanceof ForLoopTree forTree) {
            buildFor(forTree);
        } else if (stmt instanceof ReturnTree ret) {
            Node value = buildExpr(ret.expression());
            Node retNode = newReturn(value);
            graph.registerSuccessor(currentBlock, retNode);
            currentBlock().addNode(retNode);

            graph.registerSuccessor(retNode, graph.endBlock());
            currentBlock().addCfgSuccessor(graph.endBlock());
            // No more code should be generated in this block after return
            currentBlock = null;
        } else if (stmt instanceof BreakTree) {
            if (breakTargetStack.isEmpty()) {
                throw new IllegalStateException("'break' used outside of loop");
            }
            Block target = breakTargetStack.peek();
            graph.registerSuccessor(currentBlock, target);
            currentBlock().addCfgSuccessor(target);
            currentBlock = null;
        } else if (stmt instanceof ContinueTree) {
            if (continueTargetStack.isEmpty()) {
                throw new IllegalStateException("'continue' used outside of loop");
            }
            Block target = continueTargetStack.peek();
            graph.registerSuccessor(currentBlock, target);
            currentBlock().addCfgSuccessor(target);
            currentBlock = null;
        } else {
            throw new UnsupportedOperationException("Unknown StatementTree: " + stmt);
        }
    }

    private void buildIf(IfTree ifTree) {
        Block thenBlock = new Block(graph);
        Block afterBlock = new Block(graph);
        Block elseBlock = ifTree.elseBranch() != null ? new Block(graph) : afterBlock;

        Node condValue = buildExpr(ifTree.condition());
        // Condition jump: If condValue != 0 goto thenBlock else elseBlock
        Node branchNode = new CondJumpNode(currentBlock, condValue, thenBlock, elseBlock);
        currentBlock().addNode(branchNode);
        graph.registerSuccessor(currentBlock, branchNode);

        // CFG for L2
        currentBlock().addCfgSuccessor(thenBlock);
        currentBlock().addCfgSuccessor(elseBlock);

        // Then branch
        currentBlock = thenBlock;
        buildBody(ifTree.thenBranch());
        if (currentBlock != null) { // block might be null if ended by return
            graph.registerSuccessor(currentBlock, afterBlock);
            currentBlock().addCfgSuccessor(afterBlock);
        }

        // Else branch
        if (ifTree.elseBranch() != null) {
            currentBlock = elseBlock;
            buildBody(ifTree.elseBranch());
            if (currentBlock != null) {
                graph.registerSuccessor(currentBlock, afterBlock);
                currentBlock().addCfgSuccessor(afterBlock);
            }
        }
        sealBlock(thenBlock);
        if (ifTree.elseBranch() != null) {
            sealBlock(elseBlock);
        }
        sealBlock(afterBlock);
        // Continue in afterBlock
        currentBlock = afterBlock;
    }

    private void buildWhile(WhileLoopTree whileTree) {
        Block condBlock = new Block(graph);
        Block bodyBlock = new Block(graph);
        Block afterBlock = new Block(graph);

        breakTargetStack.push(afterBlock);
        continueTargetStack.push(condBlock);

        // Jump to cond block
        currentBlock().addCfgSuccessor(condBlock);
        graph.registerSuccessor(currentBlock, condBlock);
        currentBlock = condBlock;

        Node condValue = buildExpr(whileTree.condition());
        Node condJump = new CondJumpNode(currentBlock, condValue, bodyBlock, afterBlock);
        currentBlock().addNode(condJump);
        graph.registerSuccessor(currentBlock, condJump);

        currentBlock().addCfgSuccessor(bodyBlock);
        currentBlock().addCfgSuccessor(afterBlock);

        // Body
        currentBlock = bodyBlock;
        buildBody(whileTree.body());
        if (currentBlock != null) {
            // Loop back to cond block
            graph.registerSuccessor(currentBlock, condBlock);
            currentBlock().addCfgSuccessor(condBlock);
        }
        sealBlock(condBlock);
        sealBlock(bodyBlock);
        sealBlock(afterBlock);
        continueTargetStack.pop();
        breakTargetStack.pop();
        // Continue in after block
        currentBlock = afterBlock;
    }

    private void buildFor(ForLoopTree forTree) {
        Block condBlock = new Block(graph);
        Block bodyBlock = new Block(graph);
        Block stepBlock = new Block(graph);
        Block afterBlock = new Block(graph);

        breakTargetStack.push(afterBlock);
        continueTargetStack.push(stepBlock);

        // for (init; cond; step) {body}
        if (forTree.init() != null)
            buildBody(forTree.init());
        graph.registerSuccessor(currentBlock, condBlock);
        currentBlock().addCfgSuccessor(condBlock);
        currentBlock = condBlock;

        Node condValue = buildExpr(forTree.condition());
        Node condJump = new CondJumpNode(currentBlock, condValue, bodyBlock, afterBlock);
        currentBlock().addNode(condJump);
        graph.registerSuccessor(currentBlock, condJump);
        currentBlock().addCfgSuccessor(bodyBlock);
        currentBlock().addCfgSuccessor(afterBlock);

        currentBlock = bodyBlock;
        buildBody(forTree.body());
        if (currentBlock != null) {
            graph.registerSuccessor(currentBlock, stepBlock);
            currentBlock().addCfgSuccessor(stepBlock);
        }

        currentBlock = stepBlock;
        if (forTree.step() != null)
            buildBody(forTree.step());
        graph.registerSuccessor(currentBlock, condBlock);
        currentBlock().addCfgSuccessor(condBlock);

        sealBlock(condBlock);
        sealBlock(stepBlock);
        sealBlock(bodyBlock);
        sealBlock(afterBlock);

        continueTargetStack.pop();
        breakTargetStack.pop();

        currentBlock = afterBlock;
    }

    private Node buildExpr(ExpressionTree expr) {
        // debug
        // System.out.println("DEBUG: buildExpr: " + expr.getClass() + " : " + expr);
        switch (expr) {
            case LiteralTree lit -> {
                final int value;
                if (lit.base() == 16) {
                    String digits = lit.value();
                    if (digits.startsWith("0x") || digits.startsWith("0X")) {
                        digits = digits.substring(2);
                    }
                    long v = Long.parseUnsignedLong(digits, 16);
                    if ((v & ~0xFFFF_FFFFL) != 0)           // > 32-bit
                        throw new NumberFormatException("hex literal out of 32-bit range: " + lit.value());
                    value = (int) v;
                } else {
                    long v = Long.parseLong(lit.value());
                    if (v < 0 || v > 2147483648L) {
                        throw new NumberFormatException("decimal literal out of 32-bit range: " + lit.value());
                    }
                    value = (int) (v & 0xFFFF_FFFFL);
                }
                return newConstInt(value);
            }
            case IdentExpressionTree ident -> {
                Name name = ident.name().name(); // the name of NameTree
                return readVariable(name, currentBlock());
            }
            case NegateTree neg -> {
                Node operand = buildExpr(neg.expression());
                Node n = newSub(newConstInt(0), operand);
                currentBlock().addNode(n);
                return n;
            }
            case BooleanLiteralTree bl -> {
                // represent true==1, false==0
                Node c = newConstInt(bl.value() ? 1 : 0);
                currentBlock().addNode(c);
                return c;
            }
            case LogicalNotTree not -> {
                Node op = buildExpr(not.operand());
                Node n = newLogicalNot(op);
                currentBlock().addNode(n);
                return n;
            }
            case BitwiseNotTree bit -> {
                Node op = buildExpr(bit.operand());
                Node n = newBitwiseNot(op);
                currentBlock().addNode(n);
                return n;
            }
            case BinaryOperationTree bin -> {
                Node left = buildExpr(bin.lhs());
                Node right = buildExpr(bin.rhs());
                Node n = switch (bin.operatorType()) {
                    case PLUS -> newAdd(left, right);
                    case MINUS -> newSub(left, right);
                    case MUL -> newMul(left, right);
                    case DIV -> newDiv(left, right);
                    case MOD -> newMod(left, right);
                    // L2
                    case LSHIFT   -> {                        // <<
                        if (bin.rhs() instanceof LiteralTree lit) {
                            int amt = Integer.parseInt(lit.value()) & 0x1F;
                            right   = newConstInt(amt);
                        }
                        yield newShl(left, right);
                    }
                    case RSHIFT   -> {                        // >>
                        if (bin.rhs() instanceof LiteralTree lit) {
                            int amt = Integer.parseInt(lit.value()) & 0x1F;
                            right   = newConstInt(amt);
                        }
                        yield newShr(left, right);
                    }
                    case AND   -> newAnd(left, right);     // &
                    case OR    -> newOr (left, right);     // |
                    case XOR   -> newXor(left, right);     // ^
                    case LT    -> newCmpLT(left, right);   // <
                    case LE    -> newCmpLE(left, right);   // <=
                    case GT    -> newCmpGT(left, right);   // >
                    case GE    -> newCmpGE(left, right);   // >=
                    case EQ    -> newCmpEQ(left, right);   // ==
                    case NE    -> newCmpNE(left, right);   // !=
                    default -> throw new UnsupportedOperationException("Unknown binary op: " + bin.operatorType());
                };
                currentBlock().addNode(n);
                return n;
            }
            case ConditionalTree cond -> {
                // SSA: Build blocks for then/else, then merge with a Phi node in afterBlock
                Block thenBlock = new Block(graph);
                Block elseBlock = new Block(graph);
                Block afterBlock = new Block(graph);

                currentBlock().addCfgSuccessor(thenBlock);
                currentBlock().addCfgSuccessor(elseBlock);

                Node condValue = buildExpr(cond.condition());
                Node branchNode = new CondJumpNode(currentBlock, condValue, thenBlock, elseBlock);
                currentBlock().addNode(branchNode);
                graph.registerSuccessor(currentBlock, branchNode);

                // Then branch
                currentBlock = thenBlock;
                Node thenResult = buildExpr(cond.thenExpr());
                currentBlock().addNode(thenResult);
                graph.registerSuccessor(currentBlock, afterBlock);
                currentBlock().addCfgSuccessor(afterBlock);

                // Else branch
                currentBlock = elseBlock;
                Node elseResult = buildExpr(cond.elseExpr());
                currentBlock().addNode(elseResult);
                graph.registerSuccessor(currentBlock, afterBlock);
                currentBlock().addCfgSuccessor(afterBlock);

                // Merge with Phi
                currentBlock = afterBlock;
                Phi phi = new Phi(currentBlock);
                phi.appendOperand(thenResult);
                phi.appendOperand(elseResult);
                currentBlock().addNode(phi);
                return phi;
            }
            case null, default -> throw new UnsupportedOperationException("Unknown ExpressionTree: " + expr);
        }
    }

    private Name extractName(LValueTree lvalue) {
        if (lvalue instanceof LValueIdentTree idTree) {
            return idTree.name().name();
        }
        throw new UnsupportedOperationException("Only simple lvalue supported");
    }
}
