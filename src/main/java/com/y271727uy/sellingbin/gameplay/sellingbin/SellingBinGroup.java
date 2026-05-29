package com.y271727uy.sellingbin.gameplay.sellingbin;

import com.y271727uy.sellingbin.recipe.SellingBinRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SellingBinGroup {
    private final List<Entry> entries = new ArrayList<>();

    public void addRecipe(SellingBinRecipe recipe) {
        Set<ResourceLocation> seenPriceKeys = new LinkedHashSet<>();
        for (ItemStack matchingItem : recipe.getInputChoices()) {
            if (matchingItem.isEmpty()) continue;
            ResourceLocation priceKey = recipe.getPriceKey(matchingItem);
            if (!seenPriceKeys.add(priceKey)) continue;
            entries.add(new Entry(recipe, priceKey));
        }
    }

    public boolean isEmpty() { return entries.isEmpty(); }
    public List<Entry> getEntries() { return new ArrayList<>(entries); }
    public record Entry(SellingBinRecipe recipe, ResourceLocation priceKey) { }
}

