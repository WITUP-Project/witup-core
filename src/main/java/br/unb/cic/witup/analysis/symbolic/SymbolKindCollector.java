package br.unb.cic.witup.analysis.symbolic;

import java.util.HashMap;
import java.util.Map;

public final class SymbolKindCollector implements SymExprVisitor<Void> {
  private final Map<String, SymKind> symbolKindTable = new HashMap<>();

  public Map<String, SymKind> collect(final SymExpr expr) {
    expr.accept(this);
    return symbolKindTable;
  }

  @Override
  public Void visitVar(final SymVar v) {
    symbolKindTable.put(v.getName(), v.kind());
    return null;
  }

  @Override
  public Void visitBinOp(final SymBinOp b) {
    b.getLeft().accept(this);
    b.getRight().accept(this);
    return null;
  }

  @Override
  public Void visitConst(final SymConst c) {
    return null;
  }

  @Override
  public Void visitField(final SymField f) {
    f.getBase().accept(this);
    return null;
  }

  @Override
  public Void visitStringConst(final SymStringConst s) {
    return null;
  }

  @Override
  public Void visitVirtualInvoke(final SymVirtualInvoke v) {
    v.getBase().accept(this);
    symbolKindTable.put(v.toString(), v.kind());
    return null;
  }

  @Override
  public Void visitArrayRef(SymArrayRef r) {
    r.getBase().accept(this);
    symbolKindTable.put(r.toString(), r.kind());
    return null;
  }

  @Override
  public Void visitLength(SymLength l) {
    l.getOp().accept(this);
    symbolKindTable.put(l.toString(), l.kind());
    return null;
  }

  @Override
  public Void visitNewArray(SymNewArray r) {
    return null;
  }

  @Override
  public Void visitCast(SymCast c) {
    symbolKindTable.put(c.toString(), c.kind());
    return null;
  }
}
