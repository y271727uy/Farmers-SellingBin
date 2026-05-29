package com.y271727uy.sellingbin.economic;

public record CheckoutResult(boolean success, double totalPrice, double reputation, String message) {
    public static CheckoutResult success(double totalPrice, double reputation, String message) {
        return new CheckoutResult(true, totalPrice, reputation, message);
    }

    public static CheckoutResult failure(String message) {
        return new CheckoutResult(false, 0D, 0D, message == null ? "" : message);
    }
}

