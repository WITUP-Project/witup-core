package br.unb.cic.witup.solver;

public sealed interface ModelValue permits ModelValue.IntValue, ModelValue.BoolValue, ModelValue.StringValue {
  record IntValue(int value) implements ModelValue {
    public int getValue() { return value; }
  }

  record BoolValue(boolean value) implements ModelValue {
    public boolean getValue() { return value; }
  }

  record StringValue(String value) implements ModelValue {
    public String getValue() { return value; }
  }
}
