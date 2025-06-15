package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.visitor.NoOpVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

/// Checks that functions return.
/// Currently only works for straight-line code.
class ReturnAnalysis implements NoOpVisitor<Unit> {

    static class ReturnState {
        boolean returns = false;
    }

    @Override
    public Unit visit(FunctionTree functionTree, Unit data) {
        if (!alwaysReturns(functionTree.body())) {
            throw new SemanticException("function " + functionTree.name() + " does not return");
        }
        return Unit.INSTANCE;
    }

    private static boolean alwaysReturns(StatementTree stmt) {
        return switch (stmt) {
            case ReturnTree r -> true;
            case BlockTree b -> {
                boolean ret = false;
                for (StatementTree s : b.statements()) {
                    ret = alwaysReturns(s);
                    if (ret) {
                        break;
                    }
                }
                yield ret;
            }
            case IfTree i -> i.elseBranch() != null
                    && alwaysReturns(i.thenBranch())
                    && alwaysReturns(i.elseBranch());
            case WhileLoopTree w -> {
                alwaysReturns(w.body());
                yield false;
            }
            case ForLoopTree f -> {
                if (f.init() != null && alwaysReturns(f.init())) {
                    yield true;
                }
                if (f.step() != null) {
                    alwaysReturns(f.step());
                }
                // guaranteed to return
                alwaysReturns(f.body());
                yield false;
            }
            default -> false;
        };
    }
}
