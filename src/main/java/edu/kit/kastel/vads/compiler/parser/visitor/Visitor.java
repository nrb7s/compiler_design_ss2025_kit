package edu.kit.kastel.vads.compiler.parser.visitor;

import edu.kit.kastel.vads.compiler.parser.ast.*;

public interface Visitor<T, R> {

    R visit(AssignmentTree assignmentTree, T data);

    R visit(BinaryOperationTree binaryOperationTree, T data);

    R visit(BlockTree blockTree, T data);

    R visit(DeclarationTree declarationTree, T data);

    R visit(FunctionTree functionTree, T data);

    R visit(IdentExpressionTree identExpressionTree, T data);

    R visit(LiteralTree literalTree, T data);

    R visit(LValueIdentTree lValueIdentTree, T data);

    R visit(NameTree nameTree, T data);

    R visit(NegateTree negateTree, T data);

    R visit(ProgramTree programTree, T data);

    R visit(ReturnTree returnTree, T data);

    R visit(TypeTree typeTree, T data);

    // L2
    R visit(IfTree ifTree, T data);

    R visit(WhileLoopTree whileLoopTree, T data);

    R visit(ForLoopTree forLoopTree, T data);

    R visit(BreakTree breakTree, T data);

    R visit(ContinueTree continueTree, T data);

    R visit(ConditionalTree conditionalTree, T data);

    R visit(LogicalNotTree logicalNotTree, T data);

    R visit(BitwiseNotTree bitwiseNotTree, T data);

    R visit(ExpressionStatementTree expressionStatementTree, T data);

    R visit(BooleanLiteralTree booleanLiteralTree, T data);

    // L3
    R visit(ParameterTree parameterTree, T data);
}
