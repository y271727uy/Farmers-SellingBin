package com.y271727uy.sellingbin.api.economic;

import com.y271727uy.sellingbin.economic.CheckoutInput;
import com.y271727uy.sellingbin.economic.CheckoutResult;
import com.y271727uy.sellingbin.economic.CurrencyStackFactory;
import com.y271727uy.sellingbin.economic.CustomerCheckout;
import com.y271727uy.sellingbin.economic.DefaultCurrencyStackFactory;
import com.y271727uy.sellingbin.economic.Price;

@SuppressWarnings("unused")
public final class ShopcoreCheckout {
    private ShopcoreCheckout() {
    }

    public static CheckoutResult checkout(CheckoutInput input) {
        return CustomerCheckout.checkout(input, DefaultCurrencyStackFactory.INSTANCE);
    }

    public static CheckoutResult checkout(CheckoutInput input, CurrencyStackFactory stackFactory) {
        return CustomerCheckout.checkout(input, stackFactory);
    }

    public static CheckoutResult checkout(Price summarizedPrice, int quantity, double multiplier) {
        return checkout(new CheckoutInput(
                summarizedPrice.basicPrice(),
                summarizedPrice.addPrice(),
                summarizedPrice.reputation(),
                quantity,
                multiplier
        ));
    }

    public static double checkoutReputation(CheckoutInput input) {
        return ShopcoreReputation.calculateCheckoutReputation(input);
    }

    public static double checkoutReputation(Price summarizedPrice, int quantity, double multiplier) {
        return ShopcoreReputation.calculateCheckoutReputation(summarizedPrice, quantity, multiplier);
    }
}
