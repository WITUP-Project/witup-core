package br.unb.cic.witup.samples;

import java.util.function.Supplier;

// test operations on the primitive type int
public class Int {
  public int add(int a, int b) {
    if (a + b > 256) {
      throw new IllegalArgumentException("a + b > 256 overflows");
    }
    return a + b;
  }

  public int greaterThanConstantRhs(int a) {
    if (a < 0) {
      throw new IllegalArgumentException("a must be positive");
    }
    return a;
  }

  public int lesserThanConstantLhs(int a) {
    if (0 > a) {
      throw new IllegalArgumentException("a must be positive");
    }
    return a;
  }

  public int equalsConstantRhs(int a) {
    if (a == 0) {
      throw new IllegalArgumentException("a must not be zero");
    }
    return a;
  }

  public int equalsConstantLhs(int a) {
    if (0 == a) {
      throw new IllegalArgumentException("a must not be zero");
    }
    return a;
  }

  public int negatedLessThanConstantRhs(int a) {
    if (!(a > 0)) {
      throw new IllegalArgumentException("a must be positive");
    }
    return a;
  }

  public int lessThanConstantRhsViaBoolean(int a) {
    boolean invalid = a < 0;
    if (invalid) {
      throw new IllegalArgumentException("a cannot be negative");
    }
    return a;
  }

  public int lessThanConstantRhsViaNegatedBoolean(int a) {
    boolean invalid = a < 0;
    if (!invalid) {
      throw new IllegalArgumentException("a cannot be positive");
    }
    return a;
  }

  public int addAndCheck(int a, int b) {
    // calle throws if a + b > 256
    int result = add(a, b);
    // so it is not possible for result > 512 to throw
    if (result > 512) {
      throw new IllegalArgumentException("result overflows");
    }
    return result;
  }

  public int negateValueCallee(int a) {
    if (a == 0) {
      throw new IllegalArgumentException("a must not be zero");
    }
    if (a > 0) {
      return -1 * a;
    }
    return a * -1;
  }

  public int negateValue(int a) {
    int b = negateValueCallee(a);
    if (b < 0) {
      throw new IllegalArgumentException("a must not be negative");
    }
    return b;
  }

  public int applyAndCheck(int x) {
    Supplier<Integer> s =
        () -> {
          if (x < 0) {
            throw new IllegalArgumentException("x must not be negative");
          }
          return x * 2;
        };
    return s.get();
  }

  public int applyAndCheckResult(int x) {
    Supplier<Integer> s = () -> x * 2;
    int result = s.get();
    if (result < 0) {
      throw new IllegalArgumentException("negative result");
    }
    return result;
  }

  public static int staticAddGte(int a, int b) {
    if (a + b > 256) {
      throw new IllegalArgumentException("overflow");
    }
    return a + b;
  }

  public int callStaticAdd(int a, int b) {
    // calle throws if a + b > 256
    int result = staticAddGte(a, b);
    // this one cannot throw
    if (result > 512) {
      throw new IllegalArgumentException("overflow");
    }
    return result;
  }

  public int staticAddLte(int a, int b) {
    if (a + b <= 256) {
      throw new IllegalArgumentException("underflow");
    }
    return a + b;
  }

  public int callStaticAddLte(int a, int b) {
    int result = staticAddLte(a, b);
    if (result > 512) {
      throw new IllegalArgumentException("underflow");
    }
    return result;
  }

  public int alwaysThrows() {
    throw new IllegalStateException("unsupported");
  }
}
