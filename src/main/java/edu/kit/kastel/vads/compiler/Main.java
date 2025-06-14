package edu.kit.kastel.vads.compiler;

import edu.kit.kastel.vads.compiler.backend.aasm.CodeGenerator;
import edu.kit.kastel.vads.compiler.ir.*;
import edu.kit.kastel.vads.compiler.ir.optimize.LocalValueNumbering;
import edu.kit.kastel.vads.compiler.ir.util.YCompPrinter;
import edu.kit.kastel.vads.compiler.lexer.Lexer;
import edu.kit.kastel.vads.compiler.parser.ParseException;
import edu.kit.kastel.vads.compiler.parser.Parser;
import edu.kit.kastel.vads.compiler.parser.TokenSource;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.semantic.SemanticAnalysis;
import edu.kit.kastel.vads.compiler.semantic.SemanticException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Invalid arguments: Expected one input file and one output file");
            System.exit(3);
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);
        output = Path.of(output.toString() + ".s"); // duck
        ProgramTree program = lexAndParse(input);

        // introduce elaborator
        List<FunctionTree> newFuncs = new ArrayList<>();
        for (FunctionTree f : program.topLevelTrees()) {
            newFuncs.add(Elaborator.elaborate(f));
        }
        program = new ProgramTree(newFuncs);
        // debug
        // System.out.println("DEBUG: elaborated program = " + program);

        try {
            new SemanticAnalysis(program).analyze();
        } catch (SemanticException e) {
            e.printStackTrace();
            System.exit(7);
            return;
        }

        // main() check
        /*
        for (FunctionTree f : program.topLevelTrees()) {
            System.out.println("Function name: " + f.name().name());
            System.out.println("Return type: " + f.returnType());
        }
         */
        boolean hasMain = program.topLevelTrees().stream().anyMatch(f ->
                f.name().name().asString().equals("main") &&
                        f.returnType().type().toString().equals("INT")
        );
        if (!hasMain) {
            // System.err.println("Error: `int main()` entry point not found.");
            System.exit(42);
        }

        List<IrGraph> graphs = new ArrayList<>();
        for (FunctionTree function : program.topLevelTrees()) {
            // SsaTranslation translation = new SsaTranslation(function, new LocalValueNumbering());
            // IrGraph g = translation.translate();
            IrGraph g = GraphConstructor.build(function);
            PhiElimination.run(g); // phi eliminate

            // debug dump
            // System.out.println("==== IR Graph for function " + g.name() + " ====");
            // System.out.println(YCompPrinter.print(g));
            // ends

            graphs.add(g);
        }

        if ("vcg".equals(System.getenv("DUMP_GRAPHS")) || "vcg".equals(System.getProperty("dumpGraphs"))) {
            Path tmp = output.toAbsolutePath().resolveSibling("graphs");
            Files.createDirectories(tmp);
            for (IrGraph graph : graphs) {
                dumpGraph(graph, tmp, "before-codegen");
            }
        }

        // TODO: generate assembly and invoke gcc instead of generating abstract assembly
        // String s = new CodeGenerator().generateCode(graphs);
        // for test run "./run.sh ./userTest/test.vads ./userTest/test.s" after built
        String s = new CodeGenerator().generateAssembly(graphs);
        Files.writeString(output, s);

        // invoke gcc
        String outputStr = output.toString();
        String outputExecutable = outputStr.substring(0, outputStr.length() - 2);

        Process gcc = new ProcessBuilder("gcc", "-o", outputExecutable, output.toString())
                .inheritIO()
                .start();
        try {
            int exitCode = gcc.waitFor();
            if (exitCode != 0) {
                System.err.println("gcc failed with exit code " + exitCode);
                System.exit(8);
            }
        } catch (InterruptedException e) {
            System.err.println("gcc was interrupted");
            System.exit(9);
        }
    }

    private static ProgramTree lexAndParse(Path input) throws IOException {
        try {
            Lexer lexer = Lexer.forString(Files.readString(input));
            TokenSource tokenSource = new TokenSource(lexer);
            Parser parser = new Parser(tokenSource);
            return parser.parseProgram();
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(42);
            throw new AssertionError("unreachable");
        }
    }

    private static void dumpGraph(IrGraph graph, Path path, String key) throws IOException {
        Files.writeString(
            path.resolve(graph.name() + "-" + key + ".vcg"),
            YCompPrinter.print(graph)
        );
    }
}
