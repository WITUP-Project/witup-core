package br.unb.cic.witup.solver.model;

import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.FuncDecl;
import com.microsoft.z3.Model;

public final class ObjectValue implements ModelValue {
  private final Expr<?> objExpr;
  private final Model model;
  private final Context ctx;

  public ObjectValue(final Expr<?> objExpr, final Model model, final Context ctx) {
    this.objExpr = objExpr;
    this.model = model;
    this.ctx = ctx;
  }

  /**
   * Example:
   * (declare-fun field_value (java.lang.Object) Int)
   * is a declaration. Object field mappings are function declarations
   * In the case of Array.getObjectFromArray, the mapping becomes
   * java.lang.Object (all custom objects e.g MyObject become Object)
   * and since we are comparing with an int, the function range is Int.
   *
   * @param fieldName the name of the field we are trying to get
   * @return ModelValue of the respective type
   */
  @Override
  public ModelValue getField(final String fieldName) {
    // Find the field_<name> function in the model and apply it to this object
    for (FuncDecl<?> decl : model.getDecls()) {
      if (decl.getName().toString().equals("field_" + fieldName) && decl.getArity() == 1) {
        Expr<?> applied = ctx.mkApp(decl, objExpr);
        Expr<?> evaluated = model.eval(applied, true);
        System.out.println(
            "getField " + fieldName + ": objExpr=" + objExpr + " evaluated=" + evaluated);
        return ModelValue.fromExpr(evaluated, model, ctx);
      }
    }
    throw new IllegalStateException("No field function for: " + fieldName);
  }
}
