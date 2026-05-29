package com.y271727uy.sellingbin.economic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum CurrencyDenomination {
    DOGE_COIN(131072, "doge_coin"),
    NAQUADAH_GT_CREDIT(1024, "naquadah_gt_credit"),
    OSMIUM_GT_CREDIT(128, "osmium_gt_credit"),
    PLATINUM_GT_CREDIT(16, "platinum_gt_credit"),
    GOLD_GT_CREDIT(8, "gold_gt_credit"),
    SILVER_GT_CREDIT(4, "silver_gt_credit"),
    CUPRONICKEL_GT_CREDIT(2, "cupronickel_gt_credit"),
    COPPER_GT_CREDIT(1, "copper_gt_credit");

    private static final List<CurrencyDenomination> DESCENDING = Arrays.asList(values());
    private final long value;
    private final String itemPath;

    CurrencyDenomination(long value, String itemPath) {
        this.value = value;
        this.itemPath = itemPath;
    }

    public long value() { return value; }
    public String itemPath() { return itemPath; }
    public long totalValue(int amount) { return value * (long) amount; }

    public static Optional<CurrencyDenomination> fromItemStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Optional.empty();
        return fromItemId(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    public static Optional<CurrencyDenomination> fromItemId(ResourceLocation itemId) {
        if (itemId == null || !"sellingbin".equals(itemId.getNamespace())) {
            return Optional.empty();
        }
        for (CurrencyDenomination denomination : values()) {
            if (denomination.itemPath.equals(itemId.getPath())) return Optional.of(denomination);
        }
        return Optional.empty();
    }

    public static List<CurrencyDenomination> descendingValues() { return DESCENDING; }
}

