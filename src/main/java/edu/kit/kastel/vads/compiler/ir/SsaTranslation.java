package edu.kit.kastel.vads.compiler.ir;

import edu.kit.kastel.vads.compiler.ir.node.*;
import edu.kit.kastel.vads.compiler.ir.optimize.Optimizer;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfo;
import edu.kit.kastel.vads.compiler.ir.util.DebugInfoHelper;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.visitor.Visitor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;

/// SSA translation as described in
/// [`Simple and Efficient Construction of Static Single Assignment Form`](https://compilers.cs.uni-saarland.de/papers/bbhlmz13cc.pdf).
///
/// This implementation also tracks side effect edges that can be used to avoid reordering of operations that cannot be
/// reordered.
///
/// We recommend to read the paper to better understand the mechanics implemented here.
public class SsaTranslation {
    private final FunctionTree function;
    private final GraphConstructor constructor;

    public void setCurrentBlock(Block block) {
        this.constructor.setCurrentBlock(block);
    }
    public Block currentBlock() {
        return this.constructor.currentBlock();
    }
    public IrGraph graph() {
        return this.constructor.graph();
    }
    public void sealBlock(Block block) {
        this.constructor.sealBlock(block);
    }

    public SsaTranslation(FunctionTree function, Optimizer optimizer) {
        this.function = function;
        this.constructor = new GraphConstructor(optimizer, function.name().name().asString());
    }

    public IrGraph translate() {
        var visitor = new SsaTranslationVisitor();
        this.function.accept(visitor, this);
        return this.constructor.graph();
    }

    private void writeVariable(Name variable, Block block, Node value) {
        this.constructor.writeVariable(variable, block, value);
    }

    private Node readVariable(Name variable, Block block) {
        return this.constructor.readVariable(variable, block);
    }

    private static class SsaTranslationVisitor implements Visitor<SsaTranslation, Optional<Node>> {
        // Loop-target stacks for break/continue
        private final Deque<Block> breakTargetStack = new ArrayDeque<>();
        private final Deque<Block> continueTargetStack = new ArrayDeque<>();

        @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
        private static final Optional<Node> NOT_AN_EXPRESSION = Optional.empty();

        private final Deque<DebugInfo> debugStack = new ArrayDeque<>();

        private void pushSpan(Tree tree) {
            this.debugStack.push(DebugInfoHelper.getDebugInfo());
            DebugInfoHelper.setDebugInfo(new DebugInfo.SourceInfo(tree.span()));
        }

        private void popSpan() {
            DebugInfoHelper.setDebugInfo(this.debugStack.pop());
        }

        @Override
        public Optional<Node> visit(AssignmentTree assignmentTree, SsaTranslation data) {
            pushSpan(assignmentTree);
            BinaryOperator<Node> desugar = switch (assignmentTree.operator().type()) {
                case ASSIGN_MINUS -> data.constructor::newSub;
                case ASSIGN_PLUS -> data.constructor::newAdd;
                case ASSIGN_MUL -> data.constructor::newMul;
                case ASSIGN_DIV -> (lhs, rhs) -> projResultDivMod(data, data.constructor.newDiv(lhs, rhs));
                case ASSIGN_MOD -> (lhs, rhs) -> projResultDivMod(data, data.constructor.newMod(lhs, rhs));
                case ASSIGN -> null;
                default ->
                    throw new IllegalArgumentException("not an assignment operator " + assignmentTree.operator());
            };

            switch (assignmentTree.lValue()) {
                case LValueIdentTree(var name) -> {
                    Node rhs = assignmentTree.expression().accept(this, data).orElseThrow();
                    if (desugar != null) {
                        rhs = desugar.apply(data.readVariable(name.name(), data.currentBlock()), rhs);
                    }
                    data.writeVariable(name.name(), data.currentBlock(), rhs);
                }
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(BinaryOperationTree binaryOperationTree, SsaTranslation data) {
            pushSpan(binaryOperationTree);
            Node lhs = binaryOperationTree.lhs().accept(this, data).orElseThrow();
            Node rhs = binaryOperationTree.rhs().accept(this, data).orElseThrow();
            Node res = switch (binaryOperationTree.operatorType()) {
                case MINUS -> data.constructor.newSub(lhs, rhs);
                case PLUS -> data.constructor.newAdd(lhs, rhs);
                case MUL -> data.constructor.newMul(lhs, rhs);
                case DIV -> projResultDivMod(data, data.constructor.newDiv(lhs, rhs));
                case MOD -> projResultDivMod(data, data.constructor.newMod(lhs, rhs));
                case GT  -> data.constructor.newCmpGT(lhs, rhs);
                case GE  -> data.constructor.newCmpGE(lhs, rhs);
                case LT  -> data.constructor.newCmpLT(lhs, rhs);
                case LE  -> data.constructor.newCmpLE(lhs, rhs);
                case EQ  -> data.constructor.newCmpEQ(lhs, rhs);
                case NE -> data.constructor.newCmpNE(lhs, rhs);
                case AND -> data.constructor.newAnd(lhs, rhs);
                case OR  -> data.constructor.newOr(lhs, rhs);
                case XOR -> data.constructor.newXor(lhs, rhs);
                case LSHIFT -> data.constructor.newShl(lhs, rhs);
                case RSHIFT -> data.constructor.newShr(lhs, rhs);
                default ->
                    throw new IllegalArgumentException("not a binary expression operator " + binaryOperationTree.operatorType());
            };
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(BlockTree blockTree, SsaTranslation data) {
            pushSpan(blockTree);
            for (StatementTree statement : blockTree.statements()) {
                statement.accept(this, data);
                // skip everything after a return in a block
                if (statement instanceof ReturnTree) {
                    break;
                }
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(DeclarationTree declarationTree, SsaTranslation data) {
            pushSpan(declarationTree);
            if (declarationTree.initializer() != null) {
                Node rhs = declarationTree.initializer().accept(this, data).orElseThrow();
                data.writeVariable(declarationTree.name().name(), data.currentBlock(), rhs);
            }
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(FunctionTree functionTree, SsaTranslation data) {
            pushSpan(functionTree);
            Node start = data.constructor.newStart();
            data.constructor.writeCurrentSideEffect(data.constructor.newSideEffectProj(start));
            functionTree.body().accept(this, data);
            popSpan();
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(IdentExpressionTree identExpressionTree, SsaTranslation data) {
            pushSpan(identExpressionTree);
            Node value = data.readVariable(identExpressionTree.name().name(), data.currentBlock());
            popSpan();
            return Optional.of(value);
        }

        @Override
        public Optional<Node> visit(LiteralTree literalTree, SsaTranslation data) {
            pushSpan(literalTree);
            Node node = data.constructor.newConstInt((int) literalTree.parseValue().orElseThrow());
            popSpan();
            return Optional.of(node);
        }

        @Override
        public Optional<Node> visit(LValueIdentTree lValueIdentTree, SsaTranslation data) {
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(NameTree nameTree, SsaTranslation data) {
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(NegateTree negateTree, SsaTranslation data) {
            pushSpan(negateTree);
            Node node = negateTree.expression().accept(this, data).orElseThrow();
            Node res = data.constructor.newSub(data.constructor.newConstInt(0), node);
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(ProgramTree programTree, SsaTranslation data) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Node> visit(ReturnTree returnTree, SsaTranslation data) {
            pushSpan(returnTree);
            Node node = returnTree.expression().accept(this, data).orElseThrow();
            Node ret = data.constructor.newReturn(node);
            data.constructor.graph().registerSuccessor(data.currentBlock(), ret);
            data.currentBlock().addNode(ret);
            data.setCurrentBlock(null);
            popSpan();
            return Optional.empty();
        }

        @Override
        public Optional<Node> visit(TypeTree typeTree, SsaTranslation data) {
            throw new UnsupportedOperationException();
        }

        // L2
        @Override
        public Optional<Node> visit(IfTree ifTree, SsaTranslation data) {
            pushSpan(ifTree);
            Node cond = ifTree.condition().accept(this, data).orElseThrow();
            Block thenBlock = new Block(data.constructor.graph());
            Block afterBlock = new Block(data.constructor.graph());
            Block elseBlock = ifTree.elseBranch() != null ? new Block(data.constructor.graph()) : afterBlock;

            Node branch = new edu.kit.kastel.vads.compiler.ir.node.CondJumpNode(data.currentBlock(), cond, thenBlock, elseBlock);
            data.currentBlock().addNode(branch);
            data.constructor.graph().registerSuccessor(data.currentBlock(), branch);

            data.constructor.sealBlock(data.currentBlock());
            data.constructor.sealBlock(thenBlock);
            data.constructor.sealBlock(elseBlock);
            data.constructor.sealBlock(afterBlock);

            data.setCurrentBlock(thenBlock);
            ifTree.thenBranch().accept(this, data);
            if (data.currentBlock() != null) {
                data.constructor.graph().registerSuccessor(data.currentBlock(), afterBlock);
            }

            if (ifTree.elseBranch() != null) {
                data.setCurrentBlock(elseBlock);
                ifTree.elseBranch().accept(this, data);
                if (data.currentBlock() != null) {
                    data.constructor.graph().registerSuccessor(data.currentBlock(), afterBlock);
                }
            }

            data.setCurrentBlock(afterBlock);

            popSpan();
            return Optional.empty();
        }

        @Override
        public Optional<Node> visit(WhileLoopTree whileLoopTree, SsaTranslation data) {
            pushSpan(whileLoopTree);

            Block condBlock = new Block(data.constructor.graph());
            Block bodyBlock = new Block(data.constructor.graph());
            Block afterBlock = new Block(data.constructor.graph());

            breakTargetStack.push(afterBlock);
            continueTargetStack.push(condBlock);

            data.graph().registerSuccessor(data.currentBlock(), condBlock);
            data.setCurrentBlock(condBlock);

            Node cond = whileLoopTree.condition().accept(this, data).orElseThrow();
            CondJumpNode cj = new CondJumpNode(condBlock, cond, bodyBlock, afterBlock);
            condBlock.addNode(cj);
            data.graph().registerSuccessor(condBlock, cj);

            data.setCurrentBlock(bodyBlock);
            whileLoopTree.body().accept(this, data);
            if (data.currentBlock() != null) {
                data.graph().registerSuccessor(data.currentBlock(), condBlock);
            }

            breakTargetStack.pop();
            continueTargetStack.pop();
            data.setCurrentBlock(afterBlock);
            popSpan();
            return Optional.empty();
        }

        @Override
        public Optional<Node> visit(ForLoopTree forLoopTree, SsaTranslation data) {
            pushSpan(forLoopTree);

            Block condBlock = new Block(data.constructor.graph());
            Block bodyBlock = new Block(data.constructor.graph());
            Block stepBlock = new Block(data.constructor.graph());
            Block afterBlock = new Block(data.constructor.graph());

            breakTargetStack.push(afterBlock);
            continueTargetStack.push(stepBlock);

            if (forLoopTree.init() != null) {
                forLoopTree.init().accept(this, data);
            }
            data.graph().registerSuccessor(data.currentBlock(), condBlock);
            data.setCurrentBlock(condBlock);

            Node cond = forLoopTree.condition() != null
                    ? forLoopTree.condition().accept(this, data).orElseThrow()
                    : data.constructor.newConstInt(1);
            CondJumpNode cj = new CondJumpNode(condBlock, cond, bodyBlock, afterBlock);
            condBlock.addNode(cj);
            data.graph().registerSuccessor(condBlock, cj);

            data.setCurrentBlock(bodyBlock);
            forLoopTree.body().accept(this, data);
            if (data.currentBlock() != null) {
                data.graph().registerSuccessor(data.currentBlock(), stepBlock);
            }

            data.setCurrentBlock(stepBlock);
            if (forLoopTree.step() != null) {
                forLoopTree.step().accept(this, data);
            }
            data.graph().registerSuccessor(data.currentBlock(), condBlock);

            // jump out
            breakTargetStack.pop();
            continueTargetStack.pop();
            data.setCurrentBlock(afterBlock);

            popSpan();
            return Optional.empty();
        }

        @Override
        public Optional<Node> visit(BreakTree breakTree, SsaTranslation data) {
            pushSpan(breakTree);
            if (breakTargetStack.isEmpty()) {
                throw new IllegalStateException("‘break’ used outside of loop");
            }
            // Jump to the current loop’s exit block
            Block target = breakTargetStack.peek();
            data.graph().registerSuccessor(data.currentBlock(), target);
            // End this block
            data.setCurrentBlock(null);
            popSpan();
            return Optional.empty();
        }

        @Override
        public Optional<Node> visit(ContinueTree continueTree, SsaTranslation data) {
            pushSpan(continueTree);
            if (continueTargetStack.isEmpty()) {
                throw new IllegalStateException("‘continue’ used outside of loop");
            }
            // Jump to the current loop’s “step” or condition block
            Block target = continueTargetStack.peek();
            data.graph().registerSuccessor(data.currentBlock(), target);
            data.setCurrentBlock(null);
            popSpan();
            return Optional.empty();
        }

        @Override
        public Optional<Node> visit(ConditionalTree conditionalTree, SsaTranslation data) {
            pushSpan(conditionalTree);

            Block thenBlock = new Block(data.constructor.graph());
            Block elseBlock = new Block(data.constructor.graph());
            Block afterBlock = new Block(data.constructor.graph());

            data.currentBlock().addCfgSuccessor(thenBlock);
            data.currentBlock().addCfgSuccessor(elseBlock);

            Node cond = conditionalTree.condition().accept(this, data).orElseThrow();
            Node branch = new edu.kit.kastel.vads.compiler.ir.node.CondJumpNode(data.currentBlock(), cond, thenBlock, elseBlock);
            data.currentBlock().addNode(branch);
            data.constructor.graph().registerSuccessor(data.currentBlock(), branch);

            data.setCurrentBlock(thenBlock);
            Node thenRes = conditionalTree.thenExpr().accept(this, data).orElseThrow();
            data.constructor.graph().registerSuccessor(data.currentBlock(), afterBlock);
            data.currentBlock().addCfgSuccessor(afterBlock);

            data.setCurrentBlock(elseBlock);
            Node elseRes = conditionalTree.elseExpr().accept(this, data).orElseThrow();
            data.constructor.graph().registerSuccessor(data.currentBlock(), afterBlock);
            data.currentBlock().addCfgSuccessor(afterBlock);

            data.setCurrentBlock(afterBlock);
            var phi = new edu.kit.kastel.vads.compiler.ir.node.Phi(afterBlock);
            phi.addInput(thenBlock, thenRes);
            phi.addInput(elseBlock, elseRes);
            afterBlock.addNode(phi);

            popSpan();
            return Optional.of(phi);
        }

        @Override
        public Optional<Node> visit(LogicalNotTree logicalNotTree, SsaTranslation data) {
            pushSpan(logicalNotTree);
            Node operand = logicalNotTree.operand().accept(this, data).orElseThrow();
            Node res = data.constructor.newLogicalNot(operand);
            popSpan();
            return Optional.of(res);
        }

        @Override
        public Optional<Node> visit(BitwiseNotTree bitwiseNottree, SsaTranslation data) {
            pushSpan(bitwiseNottree);
            Node operand = bitwiseNottree.operand().accept(this, data).orElseThrow();
            Node res = data.constructor.newBitwiseNot(operand);
            popSpan();
            return Optional.of(res);
        }

        private Node projResultDivMod(SsaTranslation data, Node divMod) {
            // make sure we actually have a div or a mod, as optimizations could
            // have changed it to something else already
            if (!(divMod instanceof DivNode || divMod instanceof ModNode)) {
                return divMod;
            }
            Node projSideEffect = data.constructor.newSideEffectProj(divMod);
            data.constructor.writeCurrentSideEffect(projSideEffect);
            return data.constructor.newResultProj(divMod);
        }

        @Override
        public Optional<Node> visit(ExpressionStatementTree exprStmt, SsaTranslation data) {
            exprStmt.expr().accept(this, data);
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(BooleanLiteralTree boolLit, SsaTranslation data) {
            pushSpan(boolLit);
            // map `true` ↦ 1, `false` ↦ 0
            int v = boolLit.value() ? 1 : 0;
            Node n = data.constructor.newConstInt(v);
            popSpan();
            return Optional.of(n);
        }

        // L3

        @Override
        public Optional<Node> visit(ParameterTree parameterTree, SsaTranslation data) {
            // Parameters have no direct IR representation in this phase
            return NOT_AN_EXPRESSION;
        }

        @Override
        public Optional<Node> visit(CallExpressionTree callExpr, SsaTranslation data) {
            pushSpan(callExpr);
            List<Node> args = new java.util.ArrayList<>();
            for (ExpressionTree arg : callExpr.arguments()) {
                args.add(arg.accept(this, data).orElseThrow());
            }
            Node call = data.constructor.newCall(callExpr.callee().name(), args);
            popSpan();
            return Optional.of(call);
        }
    }
}
