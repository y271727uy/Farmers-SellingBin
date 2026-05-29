package com.y271727uy.sellingbin.economic;

public final class CustomerCheckout {
    private CustomerCheckout() {
    }

    public static CheckoutResult checkout(CheckoutInput input, CurrencyStackFactory stackFactory) {
        return CheckoutResult.failure("checkout unavailable");
    }
}

