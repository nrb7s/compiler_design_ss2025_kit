package edu.kit.kastel.vads.compiler.parser.visitor;

import edu.kit.kastel.vads.compiler.parser.ast.*;

/// A visitor that traverses a tree in postorder
/// @param <T> a type for additional data
/// @param <R> a type for a return type
public class RecursivePostorderVisitor<T, R> implements Visitor<T, R> {
    private final Visitor<T, R> visitor;

    public RecursivePostorderVisitor(Visitor<T, R> visitor) {
        this.visitor = visitor;
    }

    @Override
    public R visit(AssignmentTree assignmentTree, T data) {
        R r = assignmentTree.lValue().accept(this, data);
        r = assignmentTree.expression().accept(this, accumulate(data, r));
        r = this.visitor.visit(assignmentTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(BinaryOperationTree binaryOperationTree, T data) {
        R r = binaryOperationTree.lhs().accept(this, data);
        r = binaryOperationTree.rhs().accept(this, accumulate(data, r));
        r = this.visitor.visit(binaryOperationTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(BlockTree blockTree, T data) {
        R r;
        T d = data;
        for (StatementTree statement : blockTree.statements()) {
            r = statement.accept(this, d);
            d = accumulate(d, r);
        }
        r = this.visitor.visit(blockTree, d);
        return r;
    }

    @Override
    public R visit(DeclarationTree declarationTree, T data) {
        R r = declarationTree.type().accept(this, data);
        r = declarationTree.name().accept(this, accumulate(data, r));
        if (declarationTree.initializer() != null) {
            r = declarationTree.initializer().accept(this, accumulate(data, r));
        }
        r = this.visitor.visit(declarationTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(FunctionTree functionTree, T data) {
        R r = functionTree.returnType().accept(this, data);
        r = functionTree.name().accept(this, accumulate(data, r));
        r = functionTree.body().accept(this, accumulate(data, r));
        r = this.visitor.visit(functionTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(IdentExpressionTree identExpressionTree, T data) {
        R r = identExpressionTree.name().accept(this, data);
        r = this.visitor.visit(identExpressionTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(LiteralTree literalTree, T data) {
        return this.visitor.visit(literalTree, data);
    }

    @Override
    public R visit(LValueIdentTree lValueIdentTree, T data) {
        R r = lValueIdentTree.name().accept(this, data);
        r = this.visitor.visit(lValueIdentTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(NameTree nameTree, T data) {
        return this.visitor.visit(nameTree, data);
    }

    @Override
    public R visit(NegateTree negateTree, T data) {
        R r = negateTree.expression().accept(this, data);
        r = this.visitor.visit(negateTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(ProgramTree programTree, T data) {
        R r;
        T d = data;
        for (FunctionTree tree : programTree.topLevelTrees()) {
            r = tree.accept(this, d);
            d = accumulate(data, r);
        }
        r = this.visitor.visit(programTree, d);
        return r;
    }

    @Override
    public R visit(ReturnTree returnTree, T data) {
        R r = returnTree.expression().accept(this, data);
        r = this.visitor.visit(returnTree, accumulate(data, r));
        return r;
    }

    @Override
    public R visit(TypeTree typeTree, T data) {
        return this.visitor.visit(typeTree, data);
    }

    protected T accumulate(T data, R value) {
        return data;
    }

    // L2
    @Override
    public R visit(IfTree ifTree, T data) {
        R r = ifTree.condition().accept(this, data);
        r = ifTree.thenBranch().accept(this, accumulate(data, r));
        if (ifTree.elseBranch() != null) {
            r = ifTree.elseBranch().accept(this, accumulate(data, r));
        }
        return this.visitor.visit(ifTree, accumulate(data, r));
    }

    @Override
    public R visit(WhileLoopTree whileLoopTree, T data) {
        R r = whileLoopTree.condition().accept(this, data);
        r = whileLoopTree.body().accept(this, accumulate(data, r));
        return this.visitor.visit(whileLoopTree, accumulate(data, r));
    }

    @Override
    public R visit(ForLoopTree forLoopTree, T data) {
        R r = null;
        T d = data;
        if (forLoopTree.init() != null) {
            r = forLoopTree.init().accept(this, d);
            d = accumulate(d, r);
        }
        if (forLoopTree.condition() != null) {
            r = forLoopTree.condition().accept(this, d);
            d = accumulate(d, r);
        }
        if (forLoopTree.step() != null) {
            r = forLoopTree.step().accept(this, d);
            d = accumulate(d, r);
        }
        r = forLoopTree.body().accept(this, d);
        r = this.visitor.visit(forLoopTree, accumulate(d, r));
        return r;
    }

    @Override
    public R visit(BreakTree breakTree, T data) {
        return this.visitor.visit(breakTree, data);
    }

    @Override
    public R visit(ContinueTree continueTree, T data) {
        return this.visitor.visit(continueTree, data);
    }

    @Override
    public R visit(ConditionalTree conditionalTree, T data) {
        R r = conditionalTree.condition().accept(this, data);
        r = conditionalTree.thenExpr().accept(this, accumulate(data, r));
        r = conditionalTree.elseExpr().accept(this, accumulate(data, r));
        return this.visitor.visit(conditionalTree, accumulate(data, r));
    }

    @Override
    public R visit(LogicalNotTree logicalNotTree, T data) {
        R r = logicalNotTree.operand().accept(this, data);
        return this.visitor.visit(logicalNotTree, accumulate(data, r));
    }

    @Override
    public R visit(BitwiseNotTree bitwiseNotTree, T data) {
        R r = bitwiseNotTree.operand().accept(this, data);
        return this.visitor.visit(bitwiseNotTree, accumulate(data, r));
    }

    @Override
    public R visit(ExpressionStatementTree exprStmt, T data) {
        R r = exprStmt.expr().accept(this, data);
        r = this.visitor.visit(exprStmt, accumulate(data, r));
        return r;
    }
}
