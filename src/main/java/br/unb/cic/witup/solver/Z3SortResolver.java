package br.unb.cic.witup.solver;

import br.unb.cic.witup.analysis.symbolic.types.SymKind;
import com.microsoft.z3.Context;
import com.microsoft.z3.Sort;

public final class Z3SortResolver {

  public static Sort resolve(SymKind kind, Context ctx) {
    return switch (kind) {
      case INT -> ctx.getIntSort();
      case BOOLEAN -> ctx.getBoolSort();
      case STRING -> ctx.getStringSort();
      default -> ctx.mkUninterpretedSort(kind.name());
    };
  }
}
