package com.y271727uy.sellingbin.economic;

import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public enum DefaultCurrencyStackFactory implements CurrencyStackFactory {
    INSTANCE;

    @Override
    public ItemStack create(double amount) {
        return ItemStack.EMPTY;
    }
}

