package br.unb.cic.witup.samples;

public class Guards {
  // Top-of-method early return. Every path to the dereference carries `name != null`.
  public static int guardedDeref(String name) {
    if (name == null) {
      return 0;
    }
    return name.length();
  }

  // Same dereference, no guard: the control for guardedDeref.
  public static int unguardedDeref(String name) {
    return name.length();
  }

  // Both branches rejoin before the dereference, so neither decision constrains it. Two facts
  // reach the deref, and what they agree on is nothing.
  public static int joinedBranches(String name, boolean flag) {
    int n;
    if (flag) {
      n = 1;
    } else {
      n = 2;
    }
    return name.length() + n;
  }

  // Two independent early returns; both decisions have to ride on the array access.
  public static char nestedGuards(String name, int i) {
    if (name == null) {
      return 'x';
    }
    if (i < 0) {
      return 'y';
    }
    return name.charAt(i);
  }

  // The decision constrains the parameter, but the dereference is of a later, unrelated value.
  // A merge that kept `name != null` and let it describe the rebound `name` would refute a real
  // NPE. The environment is what keeps them apart.
  public static int guardThenReassign(String name) {
    if (name == null) {
      name = maybeNull();
    }
    return name.length();
  }

  private static String maybeNull() {
    return null;
  }

  // Guard then repair with a value that cannot be null.
  public static int defaultOnNull(String name) {
    if (name == null) {
      name = "";
    }
    return name.length();
  }

  // A guard every path shares, then a decision they do not. When a merge has to give something
  // up, the guard is what must survive it and the flag is what must not.
  public static int guardedThenBranching(String name, boolean flag) {
    if (name == null) {
      return 0;
    }
    int n;
    if (flag) {
      n = 1;
    } else {
      n = 2;
    }
    return name.length() + n;
  }

  // Named `t`, not `name`, so a predicate that still says `t` at the caller proves the actual was
  // never substituted rather than merely coinciding.
  public static int calleeDeref(String t) {
    return t.length();
  }

  // The guard is in the caller and the dereference is in the callee. Neither half can rule this
  // out alone: the callee has no guard, and the caller does not dereference anything.
  public static int guardedCall(String name) {
    if (name == null) {
      return 0;
    }
    return calleeDeref(name);
  }

  // No guard anywhere — but if `name.length()` returned rather than throwing, `name` was not null,
  // so the dereference inside calleeDeref cannot fail either. Shape of FileDeleteStrategy.delete,
  // which reaches doDelete only through `fileToDelete.exists()`.
  public static int derefThenPass(String name) {
    int n = name.length();
    return n + calleeDeref(name);
  }

  // An array's length is never negative, so this cannot throw. Z3 only knows that if we say so:
  // a length term is otherwise an unconstrained integer with no relation to the array.
  public static int impossibleNegativeLength(int[] xs) {
    if (xs.length < 0) {
      throw new IllegalStateException("unreachable");
    }
    return xs.length;
  }

  // `copy` is allocated with exactly `src.length` elements, so an index already known to be inside
  // `src` is inside `copy`. Provable only if the length of a freshly allocated array folds to the
  // size it was allocated with — including when that size is not a literal.
  public static byte[] copyOf(byte[] src) {
    byte[] copy = new byte[src.length];
    for (int i = 0; i < src.length; i++) {
      copy[i] = src[i];
    }
    return copy;
  }

  // Reading a length dereferences the array: it can throw on a null array, and once it has
  // returned the array is known non-null, so the access below cannot fail the same way.
  public static int lengthThenIndex(int[] xs) {
    int n = xs.length;
    return xs[0] + n;
  }

  // Object.getClass() is final and returns the runtime class, never null. The dereference of the
  // result cannot fail, though the dereference of `o` itself can.
  public static int classNameLength(Object o) {
    return o.getClass().toString().length();
  }

  // A reference cast does not change whether the reference is null, so the guard above the cast
  // settles the dereference below it.
  public static int castThenDeref(Object o) {
    if (o == null) {
      return 0;
    }
    String text = (String) o;
    return text.length();
  }

  // A positive instanceof proves the value non-null, and the cast preserves that. Shape of
  // ByteOrderMark.equals.
  public static int instanceOfThenDeref(Object o) {
    if (!(o instanceof String)) {
      return 0;
    }
    return ((String) o).length();
  }

  // String is final, so String.toCharArray() cannot be overridden and never returns null: the
  // dereference below cannot fail.
  public static int charsOfString(String text) {
    return text.toCharArray().length;
  }

  // A class of our own with a method of the same name, which really can return null. Matching a
  // contract on the method name alone would wrongly rule this out; matching on the declaring type
  // does not.
  public static final class Impostor {
    public char[] toCharArray() {
      return null;
    }
  }

  public static int charsOfImpostor(Impostor impostor) {
    return impostor.toCharArray().length;
  }

  // A loop, so the pass has a cycle to bound.
  public static int totalLength(String[] names) {
    int total = 0;
    for (int i = 0; i < names.length; i++) {
      total += names[i].length();
    }
    return total;
  }
}
