package edu.kit.kastel.vads.compiler.parser;

import com.sun.source.tree.*;
import edu.kit.kastel.vads.compiler.lexer.Identifier;
import edu.kit.kastel.vads.compiler.lexer.Keyword;
import edu.kit.kastel.vads.compiler.lexer.KeywordType;
import edu.kit.kastel.vads.compiler.lexer.NumberLiteral;
import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.lexer.Separator;
import edu.kit.kastel.vads.compiler.lexer.Separator.SeparatorType;
import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.Token;
import edu.kit.kastel.vads.compiler.parser.ast.*;
import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.ContinueTree;
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

    public Parser(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
    }

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
        if (tokenSource.peek().isKeyword(KeywordType.INT)) {
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
            init = parseStatement();
        } else {
            tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        }

        ExpressionTree condition = null;
        if  (!tokenSource.peek().isSeparator(SeparatorType.SEMICOLON)) {
            condition = parseExpression();
        }
        tokenSource.expectSeparator(SeparatorType.SEMICOLON);

        StatementTree step = null;
        if (!tokenSource.peek().isSeparator(SeparatorType.PAREN_CLOSE)) {
            step = parseStatement();
        }
        tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);

        StatementTree body = parseStatement();
        return new ForLoopTree(init, condition, step, body);
    }

    private StatementTree parseDeclaration() {
        Keyword type = this.tokenSource.expectKeyword(KeywordType.INT);
        Identifier ident = this.tokenSource.expectIdentifier();
        ExpressionTree expr = null;
        if (this.tokenSource.peek().isOperator(OperatorType.ASSIGN)) {
            this.tokenSource.expectOperator(OperatorType.ASSIGN);
            expr = parseExpression();
        }
        return new DeclarationTree(new TypeTree(BasicType.INT, type.span()), name(ident), expr);
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
                case ASSIGN, ASSIGN_DIV, ASSIGN_MINUS, ASSIGN_MOD, ASSIGN_MUL, ASSIGN_PLUS -> {
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
            case Token t -> throw new ParseException("invalid factor " + t);
        };
    }

    private static NameTree name(Identifier ident) {
        return new NameTree(Name.forIdentifier(ident), ident.span());
    }
}
