package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.parser.ast.*;

import java.util.List;

/// This is a utility class to help with debugging the parser.
public class Printer {

    private final Tree ast;
    private final StringBuilder builder = new StringBuilder();
    private boolean requiresIndent;
    private int indentDepth;

    public Printer(Tree ast) {
        this.ast = ast;
    }

    public static String print(Tree ast) {
        Printer printer = new Printer(ast);
        printer.printRoot();
        return printer.builder.toString();
    }

    private void printRoot() {
        printTree(this.ast);
    }

    private void printTree(Tree tree) {
        switch (tree) {
            case BlockTree(List<StatementTree> statements, _) -> {
                print("{");
                lineBreak();
                this.indentDepth++;
                for (StatementTree statement : statements) {
                    printTree(statement);
                }
                this.indentDepth--;
                print("}");
            }
            case FunctionTree(var returnType, var name, var params, var body) -> {
                printTree(returnType);
                space();
                printTree(name);
                print("(");
                for (int i = 0; i < params.size(); i++) {
                    if (i > 0) print(", ");
                    printTree(params.get(i));
                }
                print(")");
                space();
                printTree(body);
            }
            case NameTree(var name, _) -> print(name.asString());
            case ProgramTree(var topLevelTrees) -> {
                for (FunctionTree function : topLevelTrees) {
                    printTree(function);
                    lineBreak();
                }
            }
            case TypeTree(var type, _) -> print(type.asString());
            case BinaryOperationTree(var lhs, var rhs, var op) -> {
                print("(");
                printTree(lhs);
                print(")");
                space();
                this.builder.append(op);
                space();
                print("(");
                printTree(rhs);
                print(")");
            }
            case LiteralTree(var value, _, _) -> this.builder.append(value);
            case NegateTree(var expression, _) -> {
                print("-(");
                printTree(expression);
                print(")");
            }
            case AssignmentTree(var lValue, var op, var expression) -> {
                printTree(lValue);
                space();
                this.builder.append(op);
                space();
                printTree(expression);
                semicolon();
            }
            case DeclarationTree(var type, var name, var initializer) -> {
                printTree(type);
                space();
                printTree(name);
                if (initializer != null) {
                    print(" = ");
                    printTree(initializer);
                }
                semicolon();
            }
            case ParameterTree(var t, var n) -> {
                printTree(t);
                space();
                printTree(n);
            }
            case ReturnTree(var expr, _) -> {
                print("return ");
                printTree(expr);
                semicolon();
            }
            case LValueIdentTree(var name) -> printTree(name);
            case IdentExpressionTree(var name) -> printTree(name);
            // L2
            case IfTree(var cond, var thenBr, var elseBr) -> {
                print("if ("); printTree(cond); print(") "); printTree(thenBr);
                if (elseBr != null) { space(); print("else "); printTree(elseBr); }
            }
            case WhileLoopTree(var cond, var body) -> {
                print("while ("); printTree(cond); print(") "); printTree(body);
            }
            case ForLoopTree(var init, var cond, var step, var body) -> {
                print("for (");
                if (init != null) printTree(init);
                print("; ");
                if (cond != null) printTree(cond);
                print("; ");
                if (step != null) printTree(step);
                print(") ");
                printTree(body);
            }
            case BreakTree _ -> print("break;");
            case ContinueTree _ -> print("continue;");
            case ConditionalTree(var c, var t, var e) -> {
                printTree(c); print(" ? "); printTree(t); print(" : "); printTree(e);
            }
            case LogicalNotTree(var operand) -> {
                print("!("); printTree(operand); print(")");
            }
            case BitwiseNotTree(var operand) -> {
                print("~("); printTree(operand); print(")");
            }
            default -> throw new IllegalArgumentException("Unsupported AST node: " + tree.getClass());
        }
    }

    private void print(String str) {
        if (this.requiresIndent) {
            this.requiresIndent = false;
            this.builder.append(" ".repeat(4 * this.indentDepth));
        }
        this.builder.append(str);
    }

    private void lineBreak() {
        this.builder.append("\n");
        this.requiresIndent = true;
    }

    private void semicolon() {
        this.builder.append(";");
        lineBreak();
    }

    private void space() {
        this.builder.append(" ");
    }

}
