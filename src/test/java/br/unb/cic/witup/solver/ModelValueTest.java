package br.unb.cic.witup.solver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import br.unb.cic.witup.solver.model.ArrayValue;
import br.unb.cic.witup.solver.model.ModelValue;
import com.microsoft.z3.ArrayExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.IntSort;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ModelValueTest {

  @Test
  public void anArrayValueRendersAfterItsContextIsClosed() {
    // Model values outlive the solver: the context is closed as soon as a method is solved, and
    // the JSON is written at the end of the run. A value that only reaches into Z3 when asked to
    // print itself therefore fails at serialisation, and takes the whole report with it.
    ModelValue value;
    try (Context ctx = new Context()) {
      Solver solver = ctx.mkSolver();
      ArrayExpr<IntSort, IntSort> array =
          ctx.mkArrayConst("array", ctx.getIntSort(), ctx.getIntSort());
      solver.add(ctx.mkEq(ctx.mkSelect(array, ctx.mkInt(0)), ctx.mkInt(7)));
      Assertions.assertEquals(Status.SATISFIABLE, solver.check());

      Model model = solver.getModel();
      value = ModelValue.fromExpr(model.eval(array, true), model, ctx);
      assertInstanceOf(ArrayValue.class, value, "the sample must produce an array value");
    }

    String rendered = value.jsonValue();
    assertNotNull(rendered);
    assertFalse(rendered.isBlank(), "an array value must still say something once Z3 is gone");
  }
}
