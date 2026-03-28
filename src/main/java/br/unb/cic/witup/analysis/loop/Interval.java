package br.unb.cic.witup.analysis.loop;

/**
 * Abstract value for interval analysis. Represents a set of integers as a closed range
 * [lo, hi],
 * with {@link Long#MIN_VALUE} and {@link Long#MAX_VALUE} representing negative
 * and positive infinity respectively.
 */
public sealed interface Interval {

  Interval BOTTOM = new Bottom();
  Interval TOP = new Top();

  record Bottom() implements Interval {
    @Override
    public String toString() {
      return "⊥";
    }
  }

  record Range(long lo, long hi) implements Interval {
    public Range {
      if (lo > hi) {
        throw new IllegalArgumentException("lo > hi: " + lo + " > " + hi);
      }
    }

    public boolean isConstant() {
      return lo == hi;
    }

    @Override
    public String toString() {
      String l = lo == Long.MIN_VALUE ? "-∞" : Long.toString(lo);
      String h = hi == Long.MAX_VALUE ? "+∞" : Long.toString(hi);
      return "[" + l + ", " + h + "]";
    }
  }

  record Top() implements Interval {
    @Override
    public String toString() {
      return "⊤";
    }
  }

  static Interval constant(final long value) {
    return new Range(value, value);
  }

  static Interval range(final long lo, final long hi) {
    return new Range(lo, hi);
  }

  // lattices
  static Interval join(final Interval a, final Interval b) {
    if (a instanceof Bottom) {
      return b;
    }
    if (b instanceof Bottom) {
      return a;
    }
    if (a instanceof Top || b instanceof Top) {
      return TOP;
    }
    Range ra = (Range) a;
    Range rb = (Range) b;
    return new Range(Math.min(ra.lo(), rb.lo()), Math.max(ra.hi(), rb.hi()));
  }

  static Interval widen(final Interval prev, final Interval curr) {
    if (prev instanceof Bottom) {
      return curr;
    }
    if (curr instanceof Bottom) {
      return prev;
    }
    if (prev instanceof Top || curr instanceof Top) {
      return TOP;
    }
    Range rp = (Range) prev;
    Range rc = (Range) curr;
    long lo = rc.lo() < rp.lo() ? Long.MIN_VALUE : rp.lo();
    long hi = rc.hi() > rp.hi() ? Long.MAX_VALUE : rp.hi();
    return new Range(lo, hi);
  }

  static Interval narrow(final Interval prev, final Interval curr) {
    if (prev instanceof Bottom || curr instanceof Bottom) {
      return BOTTOM;
    }
    if (prev instanceof Top) {
      return curr;
    }
    if (curr instanceof Top) {
      return prev;
    }
    Range rp = (Range) prev;
    Range rc = (Range) curr;
    long lo = rp.lo() == Long.MIN_VALUE ? rc.lo() : rp.lo();
    long hi = rp.hi() == Long.MAX_VALUE ? rc.hi() : rp.hi();
    return new Range(lo, hi);
  }

  static Interval meet(final Interval a, final Interval b) {
    if (a instanceof Bottom || b instanceof Bottom) {
      return BOTTOM;
    }
    if (a instanceof Top) {
      return b;
    }
    if (b instanceof Top) {
      return a;
    }
    Range ra = (Range) a;
    Range rb = (Range) b;
    long lo = Math.max(ra.lo(), rb.lo());
    long hi = Math.min(ra.hi(), rb.hi());
    return lo <= hi ? new Range(lo, hi) : BOTTOM;
  }

  // ── Arithmetic ──

  static Interval add(final Interval a, final Interval b) {
    if (a instanceof Bottom || b instanceof Bottom) {
      return BOTTOM;
    }
    if (a instanceof Top || b instanceof Top) {
      return TOP;
    }
    Range ra = (Range) a;
    Range rb = (Range) b;
    return new Range(saturatingAdd(ra.lo(), rb.lo()), saturatingAdd(ra.hi(), rb.hi()));
  }

  static Interval sub(final Interval a, final Interval b) {
    if (a instanceof Bottom || b instanceof Bottom) {
      return BOTTOM;
    }
    if (a instanceof Top || b instanceof Top) {
      return TOP;
    }
    Range ra = (Range) a;
    Range rb = (Range) b;
    return new Range(saturatingAdd(ra.lo(), -rb.hi()), saturatingAdd(ra.hi(), -rb.lo()));
  }

  static Interval mul(final Interval a, final Interval b) {
    if (a instanceof Bottom || b instanceof Bottom) {
      return BOTTOM;
    }
    if (a instanceof Top || b instanceof Top) {
      return TOP;
    }
    Range ra = (Range) a;
    Range rb = (Range) b;
    if (ra.lo() == Long.MIN_VALUE
        || ra.hi() == Long.MAX_VALUE
        || rb.lo() == Long.MIN_VALUE
        || rb.hi() == Long.MAX_VALUE) {
      return TOP;
    }
    long p1 = ra.lo() * rb.lo();
    long p2 = ra.lo() * rb.hi();
    long p3 = ra.hi() * rb.lo();
    long p4 = ra.hi() * rb.hi();
    return new Range(
        Math.min(Math.min(p1, p2), Math.min(p3, p4)),
        Math.max(Math.max(p1, p2), Math.max(p3, p4)));
  }

  // ── Helpers ──

  private static long saturatingAdd(final long a, final long b) {
    long result = a + b;
    if (a > 0 && b > 0 && result < 0) {
      return Long.MAX_VALUE;
    }
    if (a < 0 && b < 0 && result > 0) {
      return Long.MIN_VALUE;
    }
    return result;
  }
}
