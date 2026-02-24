package br.unb.cic.witup.analysis;

import java.util.HashMap;
import java.util.Map;

public class SymbolKindCollector implements SymExprVisitor<Void> {
  private final Map<String, SymKind> symbolKindTable = new HashMap<>();

  public Map<String, SymKind> collect(SymExpr expr) {
    expr.accept(this);
    return symbolKindTable;
  }

  @Override public Void visitVar(SymVar v) {
    symbolKindTable.put(v.getName(), v.kind());
    return null;
  }

  @Override public Void visitBinOp(SymBinOp b) {
    b.getLeft().accept(this);
    b.getRight().accept(this);
    return null;
  }

  @Override
  public Void visitConst(SymConst c) {
    return null;
  }

  @Override public Void visitField(SymField f) {
    f.getBase().accept(this);
    return null;
  }

  @Override
  public Void visitStringConst(SymStringConst s) {
    return null;
  }

  @Override public Void visitVirtualInvoke(SymVirtualInvoke v) {
    v.getBase().accept(this);
    symbolKindTable.put(v.toString(), v.kind());
    return null;
  }
}
