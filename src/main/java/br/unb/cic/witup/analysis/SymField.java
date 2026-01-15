package br.unb.cic.witup.analysis;

public class SymField extends SymExpr {
    private final SymExpr base; // e.g., "this" or another object
    private final String fieldName; // e.g., "radius"

    public SymField(SymExpr base, String fieldName) {
        this.base = base;
        this.fieldName = fieldName;
    }

    public SymExpr getBase() {
        return base;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public SymExpr substitute(String varName, SymExpr replacement) {
        // Substitute in the base expression
        SymExpr newBase = base.substitute(varName, replacement);
        if (newBase != base) {
            return new SymField(newBase, fieldName);
        }
        return this;
    }

    @Override
    public boolean contains(String varName) {
        return base.contains(varName);
    }

    @Override
    public String toString() {
        return base.toString() + "." + fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SymField)) return false;
        SymField symField = (SymField) o;
        return base.equals(symField.base) && fieldName.equals(symField.fieldName);
    }

    @Override
    public int hashCode() {
        return 31 * base.hashCode() + fieldName.hashCode();
    }
}
