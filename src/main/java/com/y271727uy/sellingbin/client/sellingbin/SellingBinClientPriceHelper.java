package com.y271727uy.sellingbin.client.sellingbin;

import com.y271727uy.sellingbin.all.ModRecipes;
import com.y271727uy.sellingbin.integration.sereneseasons.SereneSeasonsCompat;
import com.y271727uy.sellingbin.recipe.SellingBinRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@SuppressWarnings("unused")
public final class SellingBinClientPriceHelper {
    private SellingBinClientPriceHelper() {
    }

    public static Optional<SellingBinRecipe> findRecipe(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.getConnection() == null || stack.isEmpty()) {
            return Optional.empty();
        }

        return mc.getConnection().getRecipeManager().getRecipeFor(
                ModRecipes.SELLING_BIN_RECIPE_TYPE.get(),
                new SellingBinRecipe.RecipeInput(List.of(stack)),
                mc.level
        );
    }

    public static int getPriceBonus(SellingBinRecipe recipe) {
        return getPriceBonus(recipe, recipe.output.copy());
    }

    public static int getPriceBonus(SellingBinRecipe recipe, ItemStack stack) {
        ResourceLocation priceKey = recipe.getPriceKey(stack);
        long total = (long) SellingBinClientPriceCache.getFloatingPriceBonus(priceKey)
                + SellingBinClientPriceCache.getVirtualStockPriceBonus(priceKey)
                + SellingBinClientPriceCache.getSeasonalPriceBonus(priceKey)
                + SellingBinClientPriceCache.getLongTermPriceBonus(priceKey);
        if (total <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (total >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) total;
    }

    public static int getSeasonalPriceBonus(SellingBinRecipe recipe) {
        return getSeasonalPriceBonus(recipe, recipe.output.copy());
    }

    public static int getSeasonalPriceBonus(SellingBinRecipe recipe, ItemStack stack) {
        return SellingBinClientPriceCache.getSeasonalPriceBonus(recipe.getPriceKey(stack));
    }

    public static ItemStack getDisplayInput(SellingBinRecipe recipe, ItemStack stack) {
        ItemStack preview = stack.copy();
        preview.setCount(1);
        return preview;
    }

    public static ItemStack getDisplayOutput(SellingBinRecipe recipe) {
        return getDisplayOutput(recipe, recipe.output.copy());
    }

    public static ItemStack getDisplayOutput(SellingBinRecipe recipe, ItemStack stack) {
        return recipe.getDisplayOutput(getPriceBonus(recipe, stack));
    }

    public static ItemStack getPreviewOutput(SellingBinRecipe recipe) {
        ItemStack preview = recipe.output.copy();
        preview.setCount(1);
        return preview;
    }

    public static String getPriceText(SellingBinRecipe recipe, ItemStack stack) {
        return Long.toString(recipe.getRawMinOutputCount(getPriceBonus(recipe, stack)));
    }

    public static String getPriceText(SellingBinRecipe recipe) {
        return getPriceText(recipe, recipe.output.copy());
    }

    public static String getSeasonalBonusText(SellingBinRecipe recipe, ItemStack stack) {
        int seasonalBonus = SellingBinClientPriceCache.getSeasonalPriceBonus(recipe.getPriceKey(stack));
        if (seasonalBonus == 0) {
            return "";
        }
        return Component.translatable(getSeasonalBonusTranslationKey()).getString();
    }

    public static String getSeasonalBonusText(SellingBinRecipe recipe) {
        return getSeasonalBonusText(recipe, recipe.output.copy());
    }

    private static String getSeasonalBonusTranslationKey() {
        String seasonId = SereneSeasonsCompat.getCurrentSeasonId(Minecraft.getInstance().level).orElse("unknown");
        String normalized = seasonId.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "spring" -> "tooltip.sellingbin.selling_bin.season_bonus.spring";
            case "summer" -> "tooltip.sellingbin.selling_bin.season_bonus.summer";
            case "autumn", "fall" -> "tooltip.sellingbin.selling_bin.season_bonus.autumn";
            case "winter" -> "tooltip.sellingbin.selling_bin.season_bonus.winter";
            default -> "tooltip.sellingbin.selling_bin.season_bonus.unknown";
        };
    }
}

