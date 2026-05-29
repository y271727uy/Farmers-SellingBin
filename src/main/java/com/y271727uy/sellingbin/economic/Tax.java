package com.y271727uy.sellingbin.economic;

public final class Tax {
    private Tax() {
    }

    public static TaxResult calculate(long grossAmount) {
        return calculate(grossAmount, false);
    }

    public static TaxResult calculate(long grossAmount, boolean exempt) {
        long safeGross = Math.max(0L, grossAmount);
        int rate = exempt ? 0 : safeGross > 5_000L ? 15 : safeGross > 1_000L ? 5 : 0;
        long tax = safeGross * rate / 100L;
        return new TaxResult(safeGross, rate, tax, safeGross - tax, exempt);
    }

    public record TaxResult(long grossAmount, int taxRatePercent, long taxAmount, long netAmount, boolean exempt) {
    }
}
