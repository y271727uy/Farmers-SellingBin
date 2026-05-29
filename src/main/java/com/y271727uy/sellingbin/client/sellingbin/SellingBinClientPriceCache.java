package com.y271727uy.sellingbin.client.sellingbin;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public final class SellingBinClientPriceCache {
    private static Map<ResourceLocation, Integer> floatingPriceBonusByRecipe = Map.of();
    private static Map<ResourceLocation, Integer> virtualStockPriceBonusByRecipe = Map.of();
    private static Map<ResourceLocation, Integer> seasonalPriceBonusByRecipe = Map.of();
    private static Map<ResourceLocation, Integer> longTermPriceBonusByRecipe = Map.of();

    private SellingBinClientPriceCache() {
    }

    public static void applyExtendedSnapshot(
            Map<ResourceLocation, Integer> updatedFloatingSnapshot,
            Map<ResourceLocation, Integer> updatedVirtualStockSnapshot,
            Map<ResourceLocation, Integer> updatedSeasonalSnapshot,
            Map<ResourceLocation, Integer> updatedLongTermSnapshot
    ) {
        floatingPriceBonusByRecipe = Map.copyOf(new HashMap<>(updatedFloatingSnapshot));
        virtualStockPriceBonusByRecipe = Map.copyOf(new HashMap<>(updatedVirtualStockSnapshot));
        seasonalPriceBonusByRecipe = Map.copyOf(new HashMap<>(updatedSeasonalSnapshot));
        longTermPriceBonusByRecipe = Map.copyOf(new HashMap<>(updatedLongTermSnapshot));
    }

    public static int getFloatingPriceBonus(ResourceLocation recipeId) {
        return floatingPriceBonusByRecipe.getOrDefault(recipeId, 0);
    }

    public static int getPriceBonus(ResourceLocation recipeId) {
        long total = (long) floatingPriceBonusByRecipe.getOrDefault(recipeId, 0)
                + virtualStockPriceBonusByRecipe.getOrDefault(recipeId, 0)
                + seasonalPriceBonusByRecipe.getOrDefault(recipeId, 0)
                + longTermPriceBonusByRecipe.getOrDefault(recipeId, 0);
        if (total <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (total >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    public static int getVirtualStockPriceBonus(ResourceLocation recipeId) {
        return virtualStockPriceBonusByRecipe.getOrDefault(recipeId, 0);
    }

    public static int getSeasonalPriceBonus(ResourceLocation recipeId) {
        return seasonalPriceBonusByRecipe.getOrDefault(recipeId, 0);
    }

    public static int getLongTermPriceBonus(ResourceLocation recipeId) {
        return longTermPriceBonusByRecipe.getOrDefault(recipeId, 0);
    }

    public static void clear() {
        floatingPriceBonusByRecipe = Map.of();
        virtualStockPriceBonusByRecipe = Map.of();
        seasonalPriceBonusByRecipe = Map.of();
        longTermPriceBonusByRecipe = Map.of();
    }
}

