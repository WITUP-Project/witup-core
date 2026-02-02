package br.unb.cic.witup.samples;

/**
 * Minimal class to exercise GLOBAL exception paths (interprocedural inlining):
 *
 * - Root has a local throw.
 * - Callee has 2 different throws (2 local expaths).
 * - Root calls callee twice -> inlining branches: 2 * 2 = 4 global paths + 1 local = 5.
 */
public class SafeDivision {

    /**
     * Root method:
     * - local throw: numerator < 0
     * - calls validateOperand twice, causing global expath branching when inlined
     */
    public int divideWithValidatedOperands(int numerator, int denominator) {
        // local throw in the caller (local expath)
        if (numerator < 0) {
            throw new RuntimeException("numerator must be non-negative");
        }

        int safeNumerator = validateOperand(numerator);     // call site #1 (callee has 2 throws)
        int safeDenominator = validateOperand(denominator); // call site #2 (callee has 2 throws)

        return safeNumerator / safeDenominator;
    }

    /**
     * Callee with two distinct local expaths.
     */
    private int validateOperand(int value) {
        // local expath #1
        if (value == 0) {
            throw new RuntimeException("value cannot be zero");
        }
        // local expath #2
        if (value > 1000) {
            throw new RuntimeException("value is too large");
        }
        return value;
    }
}
