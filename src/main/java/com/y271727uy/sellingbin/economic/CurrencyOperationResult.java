package com.y271727uy.sellingbin.economic;

import java.util.Objects;
import java.util.OptionalDouble;

public record CurrencyOperationResult(boolean success, double delta, OptionalDouble balanceAfter, String message) {
    public CurrencyOperationResult {
        balanceAfter = Objects.requireNonNullElse(balanceAfter, OptionalDouble.empty());
        message = message == null ? "" : message;
    }

    public static CurrencyOperationResult success(double delta, OptionalDouble balanceAfter, String message) {
        return new CurrencyOperationResult(true, delta, balanceAfter, message);
    }

    public static CurrencyOperationResult failure(double delta, String message) {
        return new CurrencyOperationResult(false, delta, OptionalDouble.empty(), message);
    }
}
