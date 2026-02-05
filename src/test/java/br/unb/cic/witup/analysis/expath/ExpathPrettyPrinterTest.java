package br.unb.cic.witup.analysis.expath;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.unb.cic.witup.analysis.ResolvedThrowCondition;
import br.unb.cic.witup.analysis.SymConst;
import br.unb.cic.witup.analysis.SymVar;
import java.util.List;

import br.unb.cic.witup.analysis.expath.printer.ExpathPrettyPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExpathPrettyPrinterTest {

    private ExpathPrettyPrinter printer;

    @BeforeEach
    void setUp() {
        printer = new ExpathPrettyPrinter();
    }

    @Test
    void shouldPrintEmptyLocalExpaths() {
        String methodSig = "<A: void m()>";
        String out = printer.printLocalExpaths(methodSig, List.of());

        System.out.println("\n--- shouldPrintEmptyLocalExpaths ---");
        System.out.println(out);

        assertTrue(out.contains("=== LOCAL EXPATHS ==="));
        assertTrue(out.contains("method: " + methodSig));
        assertTrue(out.contains("(empty)"));
    }

    @Test
    void shouldPrintLocalExpathsWithSinglePath() {
        String methodSig = "<A: void m()>";
        List<List<ResolvedThrowCondition>> locals =
                List.of(List.of(new ResolvedThrowCondition(new SymVar("n <= 0"), true)));

        String out = printer.printLocalExpaths(methodSig, locals);

        System.out.println("\n--- shouldPrintLocalExpathsWithSinglePath ---");
        System.out.println(out);

        assertTrue(out.contains("=== LOCAL EXPATHS ==="));
        assertTrue(out.contains("method: " + methodSig));
        assertTrue(out.contains("G1 [size=1]"));
        assertTrue(out.contains("(n <= 0 == true)"));
    }

    @Test
    void shouldPrintGlobalExpathsBeforeBindingWithDepth() {
        String rootSig = "<A: void root(int)>";
        int depth = 2;

        List<List<ResolvedThrowCondition>> globals =
                List.of(
                        List.of(new ResolvedThrowCondition(new SymVar("n > 10000"), true)),
                        List.of(
                                new ResolvedThrowCondition(new SymVar("n > 10000"), false),
                                new ResolvedThrowCondition(new SymVar("sum == 0.0"), true)));

        String out = printer.printGlobalExpathsBeforeBinding(rootSig, depth, globals);

        System.out.println("\n--- shouldPrintGlobalExpathsBeforeBindingWithDepth ---");
        System.out.println(out);

        assertTrue(out.contains("=== GLOBAL EXPATHS (BEFORE BINDING) ==="));
        assertTrue(out.contains("root: " + rootSig));
        assertTrue(out.contains("depth: 2"));
        assertTrue(out.contains("G1 [size=1]"));
        assertTrue(out.contains("G2 [size=2]"));
        assertTrue(out.contains("(n > 10000 == true)"));
        assertTrue(out.contains("(n > 10000 == false)"));
        assertTrue(out.contains("(sum == 0.0 == true)"));
    }

    @Test
    void shouldPrintGlobalExpathsAfterBinding() {
        String rootSig = "<A: void root(int)>";
        int depth = 2;

        List<List<ResolvedThrowCondition>> bound =
                List.of(
                        List.of(new ResolvedThrowCondition(new SymVar("(count + 1) > 10000"), false)),
                        List.of(
                                new ResolvedThrowCondition(new SymVar("total == 0.0"), false),
                                new ResolvedThrowCondition(new SymVar("(count + 1) <= 0"), true)));

        String out = printer.printGlobalExpathsAfterBinding(rootSig, depth, bound);

        System.out.println("\n--- shouldPrintGlobalExpathsAfterBinding ---");
        System.out.println(out);

        assertTrue(out.contains("=== GLOBAL EXPATHS (AFTER BINDING) ==="));
        assertTrue(out.contains("root: " + rootSig));
        assertTrue(out.contains("depth: 2"));
        assertTrue(out.contains("G1 [size=1]"));
        assertTrue(out.contains("G2 [size=2]"));
        assertTrue(out.contains("((count + 1) > 10000 == false)"));
        assertTrue(out.contains("(total == 0.0 == false)"));
        assertTrue(out.contains("((count + 1) <= 0 == true)"));
    }

    @Test
    void shouldPrintMultipleConditionsInOrder() {
        String rootSig = "<A: void root(int)>";

        List<ResolvedThrowCondition> path =
                List.of(
                        new ResolvedThrowCondition(new SymVar("a"), true),
                        new ResolvedThrowCondition(new SymVar("b"), false),
                        new ResolvedThrowCondition(new SymConst(1), true));

        String out = printer.printGlobalExpathsBeforeBinding(rootSig, 1, List.of(path));

        System.out.println("\n--- shouldPrintMultipleConditionsInOrder ---");
        System.out.println(out);

        assertTrue(out.contains("(a == true) -> (b == false) -> (1 == true)"));
    }
}
