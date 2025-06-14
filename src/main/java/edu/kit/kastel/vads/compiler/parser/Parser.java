package edu.kit.kastel.vads.compiler.parser;

import com.sun.source.tree.*;
import edu.kit.kastel.vads.compiler.lexer.*;
import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.lexer.Separator.SeparatorType;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.ExpressionStatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForLoopTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileLoopTree;
import edu.kit.kastel.vads.compiler.parser.ast.LiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final TokenSource tokenSource;
    private Token lastConsumed = new ErrorToken("uninitialized", Span.DUMMY);

    public Parser(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
    }

    private Token consume() {
        lastConsumed = tokenSource.consume();
        return lastConsumed;
    }

    private Token previous() { return lastConsumed; }

    public ProgramTree parseProgram() {
        ProgramTree programTree = new ProgramTree(List.of(parseFunction()));
        if (this.tokenSource.hasMore()) {
            throw new ParseException("expected end of input but got " + this.tokenSource.peek());
        }
        return programTree;
    }

    private FunctionTree parseFunction() {
        Keyword returnType = this.tokenSource.expectKeyword(KeywordType.INT);
        Identifier identifier = this.tokenSource.expectIdentifier();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        BlockTree body = parseBlock();
        return new FunctionTree(
            new TypeTree(BasicType.INT, returnType.span()),
            name(identifier),
            body
        );
    }

    private BlockTree parseBlock() {
        Separator bodyOpen = this.tokenSource.expectSeparator(SeparatorType.BRACE_OPEN);
        List<StatementTree> statements = new ArrayList<>();
        while (!(this.tokenSource.peek() instanceof Separator sep && sep.type() == SeparatorType.BRACE_CLOSE)) {
            statements.add(parseStatement());
        }
        Separator bodyClose = this.tokenSource.expectSeparator(SeparatorType.BRACE_CLOSE);
        return new BlockTree(statements, bodyOpen.span().merge(bodyClose.span()));
    }

    private StatementTree parseStatement() { // update for control structure
        if (tokenSource.peek().isKeyword(KeywordType.INT) ||
                tokenSource.peek().isKeyword(KeywordType.BOOL)) {
            StatementTree decl = parseDeclaration();
            tokenSource.expectSeparator(SeparatorType.SEMICOLON);
            return decl;
        }
        else if (tokenSource.peek().isKeyword(KeywordType.RETURN)) {
            StatementTree ret = parseReturn();
            tokenSource.expectSeparator(SeparatorType.SEMICOLON);
            return ret;
        }
        else if (tokenSource.peek().isKeyword(KeywordType.IF)) {
            return parseIf();
        }
        else if (tokenSource.peek().isKeyword(KeywordType.WHILE)) {
            return parseWhile();
        }
        else if (tokenSource.peek().isKeyword(KeywordType.FOR)) {
            return parseFor();
        }
        else if (tokenSource.peek().isKeyword(KeywordType.CONTINUE)) {
            tokenSource.expectKeyword(KeywordType.CONTINUE);
            tokenSource.expectSeparator(SeparatorType.SEMICOLON);
            return new ContinueTree();
        }
        else if (tokenSource.peek().isKeyword(KeywordType.BREAK)) {
            tokenSource.expectKeyword(KeywordType.BREAK);
            tokenSource.expectSeparator(SeparatorType.SEMICOLON);
            return new BreakTree();
        }
        else if (tokenSource.peek().isSeparator(SeparatorType.BRACE_OPEN)) {
            return parseBlock();
        }
        else {
            StatementTree statement = parseSimple();
            tokenSource.expectSeparator(SeparatorType.SEMICOLON);
            return statement;
        }
    }

    private StatementTree parseIf() {
        tokenSource.expectKeyword(KeywordType.IF);
        tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree condition = parseExpression();
        tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        StatementTree thenBranch = parseStatement();
        StatementTree elseBranch = null;
        if (tokenSource.peek().isKeyword(KeywordType.ELSE)) {
            tokenSource.expectKeyword(KeywordType.ELSE);
            elseBranch = parseStatement();
        }
        return new IfTree(condition, thenBranch, elseBranch);
    }

    private StatementTree parseWhile() {
        tokenSource.expectKeyword(KeywordType.WHILE);
        tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree condition = parseExpression();
        tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        StatementTree body = parseStatement();
        return new WhileLoopTree(condition, body);
    }

    private StatementTree parseFor() {
        tokenSource.expectKeyword(KeywordType.FOR);
        tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);

        StatementTree init = null;
        if (!tokenSource.peek().isSeparator(SeparatorType.SEMICOLON)) {
            if (tokenSource.peek().isKeyword(KeywordType.INT)) {
                init = parseDeclaration();
            } else {
                init = parseSimple();
            }
        }
        tokenSource.expectSeparator(SeparatorType.SEMICOLON);

        ExpressionTree condition = null;
        if (!tokenSource.peek().isSeparator(SeparatorType.SEMICOLON)) {
            condition = parseExpression();
        }
        tokenSource.expectSeparator(SeparatorType.SEMICOLON);

        StatementTree step = null;
        if (!tokenSource.peek().isSeparator(SeparatorType.PAREN_CLOSE)) {
            step = parseSimple();
        }
        tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);

        StatementTree body = parseStatement();
        return new ForLoopTree(init, condition, step, body);
    }
    // End L2

    private StatementTree parseDeclaration() {
        Keyword kw = tokenSource.peek().isKeyword(KeywordType.INT)
                ? tokenSource.expectKeyword(KeywordType.INT)
                : tokenSource.expectKeyword(KeywordType.BOOL);
        BasicType type = kw.type() == KeywordType.INT
                ? BasicType.INT
                : BasicType.BOOL;
        Identifier id = tokenSource.expectIdentifier();
        ExpressionTree init = null;
        if (tokenSource.peek() instanceof Operator op
                && op.type() == OperatorType.ASSIGN) {
            tokenSource.consume();
            init = parseExpression();
        }
        return new DeclarationTree(
                new TypeTree(type, kw.span()),
                name(id),
                init
        );
    }

    private StatementTree parseSimple() {
        LValueTree lValue = parseLValue();
        Operator assignmentOperator = parseAssignmentOperator();
        ExpressionTree expression = parseExpression();
        return new AssignmentTree(lValue, assignmentOperator, expression);
    }

    private Operator parseAssignmentOperator() {
        if (this.tokenSource.peek() instanceof Operator op) {
            return switch (op.type()) {
                case ASSIGN,
                     ASSIGN_DIV,
                     ASSIGN_MINUS,
                     ASSIGN_MOD,
                     ASSIGN_MUL,
                     ASSIGN_PLUS,
                     ASSIGN_LSHIFT,
                     ASSIGN_RSHIFT,
                     ASSIGN_AND,
                     ASSIGN_OR,
                     ASSIGN_XOR -> {
                    this.tokenSource.consume();
                    yield op;
                }
                default -> throw new ParseException("expected assignment but got " + op.type());
            };
        }
        throw new ParseException("expected assignment but got " + this.tokenSource.peek());
    }

    private LValueTree parseLValue() {
        if (this.tokenSource.peek().isSeparator(SeparatorType.PAREN_OPEN)) {
            this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
            LValueTree inner = parseLValue();
            this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
            return inner;
        }
        Identifier identifier = this.tokenSource.expectIdentifier();
        return new LValueIdentTree(name(identifier));
    }

    private StatementTree parseReturn() {
        Keyword ret = this.tokenSource.expectKeyword(KeywordType.RETURN);
        ExpressionTree expression = parseExpression();
        return new ReturnTree(expression, ret.span().start());
    }

    // update for L2
    private ExpressionTree parseExpression() {
        return parseConditional();
    }

    private ExpressionTree parseConditional() {
        ExpressionTree condition = parseLogicalOr();
        if (matchOperator(OperatorType.QUESTION)) {
            ExpressionTree thenExpr = parseExpression();
            expectOperator(OperatorType.COLON);
            ExpressionTree elseExpr = parseExpression();
            return new ConditionalTree(condition, thenExpr, elseExpr);
        }
        return condition;
    }

    private ExpressionTree parseLogicalOr() {
        ExpressionTree lhs = parseLogicalAnd();
        while (matchOperator(OperatorType.OROR)) {
            lhs = new BinaryOperationTree(lhs, parseLogicalAnd(), OperatorType.OROR);
        }
        return lhs;
    }

    private ExpressionTree parseLogicalAnd() {
        ExpressionTree lhs = parseBitwiseOr();
        while (matchOperator(OperatorType.ANDAND)) {
            lhs = new BinaryOperationTree(lhs, parseBitwiseOr(), OperatorType.ANDAND);
        }
        return lhs;
    }

    private ExpressionTree parseBitwiseOr() {
        ExpressionTree lhs = parseBitwiseXor();
        while (matchOperator(OperatorType.OR)) {
            lhs = new BinaryOperationTree(lhs, parseBitwiseXor(), OperatorType.OR);
        }
        return lhs;
    }

    private ExpressionTree parseBitwiseXor() {
        ExpressionTree lhs = parseBitwiseAnd();
        while (matchOperator(OperatorType.XOR)) {
            lhs = new BinaryOperationTree(lhs, parseBitwiseAnd(), OperatorType.XOR);
        }
        return lhs;
    }

    private ExpressionTree parseBitwiseAnd() {
        ExpressionTree lhs = parseEquality();
        while (matchOperator(OperatorType.AND)) {
            lhs = new BinaryOperationTree(lhs, parseEquality(), OperatorType.AND);
        }
        return lhs;
    }

    private ExpressionTree parseEquality() {
        ExpressionTree lhs = parseRelational();
        while (true) {
            if (matchOperator(OperatorType.EQ)) {
                lhs = new BinaryOperationTree(lhs, parseRelational(), OperatorType.EQ);
            } else if (matchOperator(OperatorType.NE)) {
                lhs = new BinaryOperationTree(lhs, parseRelational(), OperatorType.NE);
            } else {
                return lhs;
            }
        }
    }

    private ExpressionTree parseRelational() {
        ExpressionTree lhs = parseShift();
        while (true) {
            if (matchOperator(OperatorType.LT)) {
                lhs = new BinaryOperationTree(lhs, parseShift(), OperatorType.LT);
            } else if (matchOperator(OperatorType.GT)) {
                lhs = new BinaryOperationTree(lhs, parseShift(), OperatorType.GT);
            } else if (matchOperator(OperatorType.LE)) {
                lhs = new BinaryOperationTree(lhs, parseShift(), OperatorType.LE);
            } else if (matchOperator(OperatorType.GE)) {
                lhs = new BinaryOperationTree(lhs, parseShift(), OperatorType.GE);
            } else {
                return lhs;
            }
        }
    }

    private ExpressionTree parseShift() {
        ExpressionTree lhs = parseAdditive();
        while (true) {
            if (matchOperator(OperatorType.LSHIFT)) {
                lhs = new BinaryOperationTree(lhs, parseAdditive(), OperatorType.LSHIFT);
            } else if (matchOperator(OperatorType.RSHIFT)) {
                lhs = new BinaryOperationTree(lhs, parseAdditive(), OperatorType.RSHIFT);
            } else {
                return lhs;
            }
        }
    }

    private ExpressionTree parseAdditive() {
        ExpressionTree lhs = parseMultiplicative();
        while (true) {
            if (matchOperator(OperatorType.PLUS)) {
                lhs = new BinaryOperationTree(lhs, parseMultiplicative(), OperatorType.PLUS);
            } else if (matchOperator(OperatorType.MINUS)) {
                lhs = new BinaryOperationTree(lhs, parseMultiplicative(), OperatorType.MINUS);
            } else {
                return lhs;
            }
        }
    }

    private ExpressionTree parseMultiplicative() {
        ExpressionTree lhs = parseUnary();
        while (true) {
            if (matchOperator(OperatorType.MUL)) {
                lhs = new BinaryOperationTree(lhs, parseUnary(), OperatorType.MUL);
            } else if (matchOperator(OperatorType.DIV)) {
                lhs = new BinaryOperationTree(lhs, parseUnary(), OperatorType.DIV);
            } else if (matchOperator(OperatorType.MOD)) {
                lhs = new BinaryOperationTree(lhs, parseUnary(), OperatorType.MOD);
            } else {
                return lhs;
            }
        }
    }

    private ExpressionTree parseUnary() {
        if (matchOperator(OperatorType.MINUS)) {
            return new NegateTree(parseUnary(), previous().span());
        } else if (matchOperator(OperatorType.NOT)) {
            return new LogicalNotTree(parseUnary());
        } else if (matchOperator(OperatorType.BITWISE_NOT)) {
            return new BitwiseNotTree(parseUnary());
        }
        return parseFactor();
    }

    private boolean matchOperator(OperatorType type) {
        if (tokenSource.peek() instanceof Operator op && op.type() == type) {
            consume(); // encapsulated tokenSource.consume()
            return true;
        }
        return false;
    }

    private void expectOperator(OperatorType type) {
        if (!matchOperator(type)) {
            throw new ParseException("Expected operator " + type);
        }
    }
    /*
    private ExpressionTree parseExpression() {
        ExpressionTree lhs = parseTerm();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _)
                && (type == OperatorType.PLUS || type == OperatorType.MINUS)) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseTerm(), type);
            } else {
                return lhs;
            }
        }
    }
     */

    private ExpressionTree parseTerm() {
        ExpressionTree lhs = parseFactor();
        while (true) {
            if (this.tokenSource.peek() instanceof Operator(var type, _)
                && (type == OperatorType.MUL || type == OperatorType.DIV || type == OperatorType.MOD)) {
                this.tokenSource.consume();
                lhs = new BinaryOperationTree(lhs, parseFactor(), type);
            } else {
                return lhs;
            }
        }
    }

    private ExpressionTree parseFactor() {
        return switch (this.tokenSource.peek()) {
            case Separator(var type, _) when type == SeparatorType.PAREN_OPEN -> {
                this.tokenSource.consume();
                ExpressionTree expression = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                yield expression;
            }
            case Operator(var type, _) when type == OperatorType.MINUS -> {
                Span span = this.tokenSource.consume().span();
                yield new NegateTree(parseFactor(), span);
            }
            case Identifier ident -> {
                this.tokenSource.consume();
                yield new IdentExpressionTree(name(ident));
            }
            case NumberLiteral(String value, int base, Span span) -> {
                this.tokenSource.consume();
                yield new LiteralTree(value, base, span);
            }
            case Keyword(var kw, var span) when kw == KeywordType.TRUE -> {
                consume();
                yield new BooleanLiteralTree(true, span);
            }
            case Keyword(var kw, var span) when kw == KeywordType.FALSE -> {
                consume();
                yield new BooleanLiteralTree(false, span);
            }
            case Keyword(var kw, var span) when kw == KeywordType.NULL -> {
                consume();
                yield new LiteralTree("NULL", 10, span);
            }
            case Token t -> throw new ParseException("invalid factor " + t);
        };
    }

    private static NameTree name(Identifier ident) {
        return new NameTree(Name.forIdentifier(ident), ident.span());
    }
}
