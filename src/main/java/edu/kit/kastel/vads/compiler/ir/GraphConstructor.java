package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class GraphConstructor {

    private final Optimizer optimizer;
    private final IrGraph graph;
    private final Map<Name, Map<Block, Node>> currentDef = new HashMap<>();
    private final Map<Block, Map<Name, Phi>> incompletePhis = new HashMap<>();
    private final Map<Block, Node> currentSideEffect = new HashMap<>();
    private final Map<Block, Phi> incompleteSideEffectPhis = new HashMap<>();
    private final Set<Block> sealedBlocks = new HashSet<>();
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
        return this.optimizer.transform(new AddNode(currentBlock(), left, right));
    }
    public Node newSub(Node left, Node right) {
        return this.optimizer.transform(new SubNode(currentBlock(), left, right));
    }

    public Node newMul(Node left, Node right) {
        return this.optimizer.transform(new MulNode(currentBlock(), left, right));
    }

    public Node newDiv(Node left, Node right) {
        return this.optimizer.transform(new DivNode(currentBlock(), left, right, readCurrentSideEffect()));
    }

    public Node newMod(Node left, Node right) {
        return this.optimizer.transform(new ModNode(currentBlock(), left, right, readCurrentSideEffect()));
    }

    public Node newReturn(Node result) {
        return new ReturnNode(currentBlock(), readCurrentSideEffect(), result);
    }

    public Node newConstInt(int value) {
        // always move const into start block, this allows better deduplication
        // and resultingly in better value numbering
        return this.optimizer.transform(new ConstIntNode(this.graph.startBlock(), value));
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

    public Node newBitwiseNot(Node operand) {
        BitwiseNotNode node = new BitwiseNotNode(currentBlock(), operand);
        currentBlock.addNode(node);
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
        return tryRemoveTrivialPhi(phi);
    }

    Node tryRemoveTrivialPhi(Phi phi) {
        // TODO: the paper shows how to remove trivial phis.
        // as this is not a problem in Lab 1 and it is just
        // a simplification, we recommend to implement this
        // part yourself.
        return phi;
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
        return tryRemoveTrivialPhi(phi);
    }

    // L2: AST -> IR
    public static IrGraph build(FunctionTree function) {
        Optimizer optimizer = new Optimizer() {
            @Override
            public Node transform(Node n) { return n; }
        };
        GraphConstructor gc = new GraphConstructor(optimizer, function.name().name().asString());
        gc.buildBody(function.body());
        return gc.graph();
    }

    private void buildBody(StatementTree stmt) {
        if (stmt instanceof BlockTree block) {
            for (StatementTree s : block.statements()) {
                buildBody(s);
            }
        } else if (stmt instanceof DeclarationTree decl) {
            // Do Nothing, just SSA setup
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
            buildWhile(whileTree);
        } else if (stmt instanceof ReturnTree ret) {
            Node value = buildExpr(ret.expression());
            Node retNode = newReturn(value);
            graph.registerSuccessor(currentBlock, retNode);
            currentBlock.addNode(retNode);
            // No more code should be generated in this block after return
            currentBlock = null;
        } else if (stmt instanceof ContinueTree || stmt instanceof BreakTree) {
            // Do nothing, handle in codegenstage
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
        currentBlock.addNode(branchNode);
        graph.registerSuccessor(currentBlock, branchNode);

        // Then branch
        currentBlock = thenBlock;
        buildBody(ifTree.thenBranch());
        if (currentBlock != null) { // block might be null if ended by return
            graph.registerSuccessor(currentBlock, afterBlock);
        }

        // Else branch
        if (ifTree.elseBranch() != null) {
            currentBlock = elseBlock;
            buildBody(ifTree.elseBranch());
            if (currentBlock != null) {
                graph.registerSuccessor(currentBlock, afterBlock);
            }
        }
        // Continue in afterBlock
        currentBlock = afterBlock;
    }

    private void buildWhile(WhileLoopTree whileTree) {
        Block condBlock = new Block(graph);
        Block bodyBlock = new Block(graph);
        Block afterBlock = new Block(graph);

        // Jump to cond block
        graph.registerSuccessor(currentBlock, condBlock);
        currentBlock = condBlock;

        Node condValue = buildExpr(whileTree.condition());
        Node condJump = new CondJumpNode(currentBlock, condValue, bodyBlock, afterBlock);
        currentBlock.addNode(condJump);
        graph.registerSuccessor(currentBlock, condJump);

        // Body
        currentBlock = bodyBlock;
        buildBody(whileTree.body());
        if (currentBlock != null) {
            // Loop back to cond block
            graph.registerSuccessor(currentBlock, condBlock);
        }
        // Continue in after block
        currentBlock = afterBlock;
    }

    private Node buildExpr(ExpressionTree expr) {
        switch (expr) {
            case LiteralTree lit -> {
                // int only
                int value = Integer.parseInt(lit.value());
                Node n = newConstInt(value);
                currentBlock.addNode(n);
                return n;
            }
            case IdentExpressionTree ident -> {
                Name name = ident.name().name(); // the name of NameTree

                return readVariable(name, currentBlock()); // the name of NameTree
            }
            case NegateTree neg -> {
                Node operand = buildExpr(neg.expression());
                Node n = newSub(newConstInt(0), operand);
                currentBlock.addNode(n);
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
                    default -> throw new UnsupportedOperationException("Unknown binary op: " + bin.operatorType());
                };
                currentBlock.addNode(n);
                return n;
            }
            case ConditionalTree cond -> {
                // SSA: Build blocks for then/else, then merge with a Phi node in afterBlock
                Block thenBlock = new Block(graph);
                Block elseBlock = new Block(graph);
                Block afterBlock = new Block(graph);

                Node condValue = buildExpr(cond.condition());
                Node branchNode = new CondJumpNode(currentBlock, condValue, thenBlock, elseBlock);
                currentBlock.addNode(branchNode);
                graph.registerSuccessor(currentBlock, branchNode);

                // Then branch
                currentBlock = thenBlock;
                Node thenResult = buildExpr(cond.thenExpr());
                currentBlock.addNode(thenResult);
                graph.registerSuccessor(currentBlock, afterBlock);

                // Else branch
                currentBlock = elseBlock;
                Node elseResult = buildExpr(cond.elseExpr());
                currentBlock.addNode(elseResult);
                graph.registerSuccessor(currentBlock, afterBlock);

                // Merge with Phi
                currentBlock = afterBlock;
                Phi phi = new Phi(currentBlock);
                phi.appendOperand(thenResult);
                phi.appendOperand(elseResult);
                currentBlock.addNode(phi);
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
