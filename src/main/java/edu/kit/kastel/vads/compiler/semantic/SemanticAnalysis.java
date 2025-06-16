package edu.kit.kastel.vads.compiler.semantic;

import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.visitor.RecursivePostorderVisitor;
import edu.kit.kastel.vads.compiler.parser.visitor.Unit;

public class SemanticAnalysis {

    private final ProgramTree program;

    public SemanticAnalysis(ProgramTree program) {
        this.program = program;
    }

    public void analyze() {
        this.program.accept(new LoopControlAnalysis(), 0);
        this.program.accept(new RecursivePostorderVisitor<>(new IntegerLiteralRangeAnalysis()), new Namespace<>());
        this.program.accept(new RecursivePostorderVisitor<>(new VariableStatusAnalysis()), new Namespace<>());
        this.program.accept(new RecursivePostorderVisitor<>(new ReturnAnalysis()), Unit.INSTANCE);
        this.program.accept(new TypeCheckAnalysis(), new Namespace<>());
    }

}
