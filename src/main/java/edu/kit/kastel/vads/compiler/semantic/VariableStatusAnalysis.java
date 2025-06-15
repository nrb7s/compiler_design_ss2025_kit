package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/// Checks that variables are
/// - declared before assignment
/// - not declared twice
/// - not initialized twice
/// - assigned before referenced
class VariableStatusAnalysis implements NoOpVisitor<Namespace<VariableStatusAnalysis.VariableStatus>> {

    @Override
    public Unit visit(ProgramTree programTree, Namespace<VariableStatus> data) {
        for (FunctionTree f : programTree.topLevelTrees()) {
            f.accept(this, new Namespace<>());
        }
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(FunctionTree functionTree, Namespace<VariableStatus> data) {
        functionTree.body().accept(this, data.fork());
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(AssignmentTree assignmentTree, Namespace<VariableStatus> data) {
        switch (assignmentTree.lValue()) {
            case LValueIdentTree(var name) -> {
                VariableStatus status = data.get(name);
                if (assignmentTree.operator().type() == Operator.OperatorType.ASSIGN) {
                    checkDeclared(name, status);
                } else {
                    checkInitialized(name, status);
                }
                if (status != VariableStatus.INITIALIZED) {
                    // only update when needed, reassignment is totally fine
                    updateStatus(data, VariableStatus.INITIALIZED, name);
                }
            }
        }
        return NoOpVisitor.super.visit(assignmentTree, data);
    }

    private static void checkDeclared(NameTree name, @Nullable VariableStatus status) {
        if (status == null) {
            throw new SemanticException("Variable " + name + " must be declared before assignment");
        }
    }

    private static void checkInitialized(NameTree name, @Nullable VariableStatus status) {
        if (status == null || status == VariableStatus.DECLARED) {
            throw new SemanticException("Variable " + name + " must be initialized before use");
        }
    }

    private static void checkUndeclared(NameTree name, @Nullable VariableStatus status) {
        if (status != null) {
            throw new SemanticException("Variable " + name + " is already declared");
        }
    }

    @Override
    public Unit visit(DeclarationTree declarationTree, Namespace<VariableStatus> data) {
        // debug
        // System.out.println("declaring " + declarationTree.name() + " status=" + data.get(declarationTree.name()));

        checkUndeclared(declarationTree.name(), data.get(declarationTree.name()));
        VariableStatus status = declarationTree.initializer() == null
            ? VariableStatus.DECLARED
            : VariableStatus.INITIALIZED;
        updateStatus(data, status, declarationTree.name());
        return NoOpVisitor.super.visit(declarationTree, data);
    }

    private static void updateStatus(Namespace<VariableStatus> data, VariableStatus status, NameTree name) {
        data.put(name, status, (existing, replacement) -> {
            if (existing.ordinal() >= replacement.ordinal()) {
                throw new SemanticException("variable is already " + existing + ". Cannot be " + replacement + " here.");
            }
            return replacement;
        });
    }

    @Override
    public Unit visit(IdentExpressionTree identExpressionTree, Namespace<VariableStatus> data) {
        VariableStatus status = data.get(identExpressionTree.name());
        checkInitialized(identExpressionTree.name(), status);
        return NoOpVisitor.super.visit(identExpressionTree, data);
    }

    enum VariableStatus {
        DECLARED,
        INITIALIZED;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    // L2
    @Override
    public Unit visit(BlockTree blockTree, Namespace<VariableStatus> data) {
        Namespace<VariableStatus> scope = data.fork();
        for (StatementTree stmt : blockTree.statements()) {
            stmt.accept(this, scope);
        }
        data.mergeExisting(scope, (oldVal, newVal) -> newVal);
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(IfTree ifTree, Namespace<VariableStatus> data) {
        ifTree.condition().accept(this, data);
        Namespace<VariableStatus> thenScope = data.fork();
        ifTree.thenBranch().accept(this, thenScope);

        Namespace<VariableStatus> elseScope = data.fork();
        if (ifTree.elseBranch() != null) {
            ifTree.elseBranch().accept(this, elseScope);
        }
        for (var name : data.names()) {
            VariableStatus sThen = thenScope.get(name);
            VariableStatus sElse = elseScope.get(name);
            if (sThen == null || sElse == null) {
                continue;
            }
            VariableStatus merged = sThen.ordinal() < sElse.ordinal() ? sThen : sElse;
            data.put(name, merged, (a, b) -> b);
        }
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(WhileLoopTree whileLoopTree, Namespace<VariableStatus> data) {
        whileLoopTree.condition().accept(this, data);
        Namespace<VariableStatus> scope = data.fork();
        whileLoopTree.body().accept(this, scope);
        for (var name : data.names()) {
            VariableStatus before = data.get(name);
            VariableStatus after = scope.get(name);
            if (before == null || after == null) {
                continue;
            }
            VariableStatus merged = before.ordinal() < after.ordinal() ? before : after;
            data.put(name, merged, (o, n) -> n);
        }
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(ForLoopTree forLoopTree, Namespace<VariableStatus> data) {
        Namespace<VariableStatus> scope = data.fork();
        if (forLoopTree.init() != null) {
            forLoopTree.init().accept(this, scope);
            data.mergeExisting(scope, (o, n) -> n);
        }
        if (forLoopTree.condition() != null) {
            forLoopTree.condition().accept(this, scope);
        }
        if (forLoopTree.step() != null) {
            forLoopTree.step().accept(this, scope);
        }
        forLoopTree.body().accept(this, scope);
        return Unit.INSTANCE;
    }

    @Override
    public Unit visit(BreakTree breakTree, Namespace<VariableStatus> data) {
        return NoOpVisitor.super.visit(breakTree, data);
    }

    @Override
    public Unit visit(ContinueTree continueTree, Namespace<VariableStatus> data) {
        return NoOpVisitor.super.visit(continueTree, data);
    }

    @Override
    public Unit visit(ConditionalTree conditionalTree, Namespace<VariableStatus> data) {
        conditionalTree.condition().accept(this, data);
        conditionalTree.thenExpr().accept(this, data);
        conditionalTree.elseExpr().accept(this, data);
        return NoOpVisitor.super.visit(conditionalTree, data);
    }

    @Override
    public Unit visit(LogicalNotTree logicalNotTree, Namespace<VariableStatus> data) {
        logicalNotTree.operand().accept(this, data);
        return NoOpVisitor.super.visit(logicalNotTree, data);
    }

    @Override
    public Unit visit(BitwiseNotTree bitwiseNotTree, Namespace<VariableStatus> data) {
        bitwiseNotTree.operand().accept(this, data);
        return NoOpVisitor.super.visit(bitwiseNotTree, data);
    }

    @Override
    public Unit visit(ExpressionStatementTree expressionStatementTree, Namespace<VariableStatus> data) {
        expressionStatementTree.expr().accept(this, data);
        return NoOpVisitor.super.visit(expressionStatementTree, data);
    }

    @Override
    public Unit visit(BooleanLiteralTree boolLit, Namespace<VariableStatus> data) {
        return NoOpVisitor.super.visit(boolLit, data);
    }
}
