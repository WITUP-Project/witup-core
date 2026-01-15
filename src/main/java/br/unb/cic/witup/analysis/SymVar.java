package br.unb.cic.witup.analysis;

public class SymVar extends SymExpr {
    private final String name;

    public SymVar(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public SymExpr substitute(String varName, SymExpr replacement) {
        if (this.name.equals(varName)) {
            return replacement;
        }
        return this;
    }

    @Override
    public boolean contains(String varName) {
        return this.name.equals(varName);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SymVar)) return false;
        SymVar symVar = (SymVar) o;
        return name.equals(symVar.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
