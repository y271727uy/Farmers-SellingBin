package com.y271727uy.sellingbin.api.economic;

import com.y271727uy.sellingbin.economic.CheckoutInput;
import com.y271727uy.sellingbin.economic.Price;

@SuppressWarnings("unused")
public final class ShopcoreReputation {
    private ShopcoreReputation() {
    }

    public static double calculateCheckoutReputation(CheckoutInput input) {
        return 0D;
    }

    public static double calculateCheckoutReputation(Price summarizedPrice, int quantity, double multiplier) {
        return 0D;
    }
}

