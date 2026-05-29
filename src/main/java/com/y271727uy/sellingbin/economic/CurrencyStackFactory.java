package com.y271727uy.sellingbin.economic;

import net.minecraft.world.item.ItemStack;

public interface CurrencyStackFactory {
    ItemStack create(double amount);
}

