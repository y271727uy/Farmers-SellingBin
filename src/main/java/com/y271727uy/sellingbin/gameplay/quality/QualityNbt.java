package com.y271727uy.sellingbin.gameplay.quality;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

public final class QualityNbt {
    private QualityNbt() {
    }

    public static int getMinPriceBonus(ItemStack stack) {
        return 0;
    }

    public static int getMaxPriceBonus(ItemStack stack) {
        return 0;
    }

    public static int rollPriceBonus(ItemStack stack, RandomSource random) {
        return 0;
    }
}

