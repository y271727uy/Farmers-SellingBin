package com.y271727uy.sellingbin.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.y271727uy.sellingbin.all.ModRecipes;
import com.y271727uy.sellingbin.client.sellingbin.SellingBinClientPriceCache;
import com.y271727uy.sellingbin.gameplay.quality.QualityNbt;
import com.y271727uy.sellingbin.gameplay.sellingbin.SellingBinGroupManager;
import com.y271727uy.sellingbin.integration.sereneseasons.SereneSeasonsCompat;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@net.minecraft.MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public class SellingBinRecipe implements Recipe<SellingBinRecipe.RecipeInput> {
    public final ResourceLocation id;
    public final Ingredient input;
    private final ItemStack[] inputChoices;
    private final int inputCount;
    public final ItemStack output;
    @Nullable public final Integer base;
    @Nullable public final Integer max;
    public final String group;
    public final boolean tradeBalance;
    public final boolean sRegression;
    public final String season;
    @Nullable public final Integer seasonBase;
    @Nullable public final Integer seasonMax;
    public final boolean seasonOnly;

    public SellingBinRecipe(ResourceLocation id, Ingredient input, int inputCount, ItemStack output, @Nullable Integer base, @Nullable Integer max, String group, boolean tradeBalance, boolean sRegression, String season, @Nullable Integer seasonBase, @Nullable Integer seasonMax, boolean seasonOnly) {
        this.id = id;
        this.input = input;
        this.inputChoices = input.getItems();
        this.inputCount = Math.max(1, inputCount);
        this.output = output;
        this.base = base;
        this.max = max;
        this.group = group;
        this.tradeBalance = tradeBalance;
        this.sRegression = sRegression;
        this.season = normalizeSeasonId(season);
        this.seasonBase = seasonBase;
        this.seasonMax = seasonMax;
        this.seasonOnly = seasonOnly && !this.season.isEmpty();
    }

    public String getGroup() { return group; }
    public boolean isMultiChoiceInput() { return inputChoices.length > 1; }
    @SuppressWarnings("deprecation")
    public ResourceLocation getPriceKey(ItemStack stack) { return stack.isEmpty() || !input.test(stack) ? id : BuiltInRegistries.ITEM.getKey(stack.getItem()); }
    public ItemStack[] getInputChoices() { return inputChoices; }
    public int getInputCount() { return inputCount; }
    public boolean isTradeBalance() { return tradeBalance; }
    public boolean isSRegression() { return sRegression; }
    public boolean hasSeason() { return !season.isEmpty(); }
    public boolean hasSeasonalPriceRange() { return seasonBase != null && seasonMax != null; }
    public boolean isSeasonOnly() { return seasonOnly && hasSeason(); }
    public boolean matchesSeason(@Nullable String currentSeasonId) { return hasSeason() && season.equals(normalizeSeasonId(currentSeasonId)); }
    public boolean isInActiveSeason(Level level) { return matchesSeason(SereneSeasonsCompat.getCurrentSeasonId(level).orElse("")); }
    public boolean canSellIn(Level level) { return !isSeasonOnly() || isInActiveSeason(level); }
    public int getConfiguredSeasonalPriceBonus(String currentSeasonId, ResourceLocation priceKey) {
        if (!hasSeasonalPriceRange() || !matchesSeason(currentSeasonId)) {
            return 0;
        }

        int minBonus = Objects.requireNonNull(seasonBase);
        int maxBonus = Objects.requireNonNull(seasonMax);
        if (maxBonus <= minBonus) {
            return minBonus;
        }

        int range = maxBonus - minBonus + 1;
        int seed = Objects.hash(id.toString(), season, minBonus, maxBonus, priceKey.toString());
        return minBonus + Math.floorMod(seed, range);
    }
    public Set<ResourceLocation> getPriceKeys() {
        Set<ResourceLocation> priceKeys = new LinkedHashSet<>();
        for (ItemStack inputChoice : inputChoices) {
            if (!inputChoice.isEmpty()) {
                priceKeys.add(getPriceKey(inputChoice));
            }
        }
        if (priceKeys.isEmpty()) {
            priceKeys.add(id);
        }
        return priceKeys;
    }
    public ItemStack getPrimaryInputPreview() { ItemStack preview = inputChoices.length == 0 ? ItemStack.EMPTY : inputChoices[0].copy(); if (!preview.isEmpty()) preview.setCount(inputCount); return preview; }
    public ItemStack pickRandomInputChoice(RandomSource random) { if (inputChoices.length == 0) return ItemStack.EMPTY; ItemStack preview = inputChoices.length == 1 ? inputChoices[0].copy() : inputChoices[random.nextInt(inputChoices.length)].copy(); preview.setCount(inputCount); return preview; }

    @Override
    public boolean matches(RecipeInput container, Level level) {
        ItemStack stack = container.getItem(0);
        return !stack.isEmpty() && input.test(stack) && canSellIn(level);
    }

    @Override
    public ItemStack assemble(RecipeInput container, RegistryAccess registryAccess) { return output.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(RegistryAccess registryAccess) { return output; }
    @Override public ResourceLocation getId() { return id; }
    @Override public RecipeSerializer<?> getSerializer() { return ModRecipes.SELLING_BIN_RECIPE_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return ModRecipes.SELLING_BIN_RECIPE_TYPE.get(); }

    public int rollOutputCount(Level level) {
        return rollOutputCount(level, getPrimaryInputPreview());
    }

    public int rollOutputCount(Level level, ItemStack inputStack) {
        int priceBonus = getPriceBonus(level, inputStack) + QualityNbt.rollPriceBonus(inputStack, level.random);
        int b = getMinOutputCount(priceBonus);
        int m = getMaxOutputCount(priceBonus);
        return b + level.random.nextInt(m - b + 1);
    }

    private int getPriceBonus(Level level, ItemStack inputStack) {
        ResourceLocation priceKey = getPriceKey(inputStack);
        if (level instanceof ServerLevel serverLevel) {
            return SellingBinGroupManager.getPriceBonus(serverLevel, priceKey);
        }
        return SellingBinClientPriceCache.getPriceBonus(priceKey);
    }

    public long getRawMinOutputCount(int priceBonus) {
        if (base == null || max == null) {
            return (long) output.getCount() + priceBonus;
        }
        return (long) base + priceBonus;
    }

    public int getMinOutputCount(int priceBonus) {
        return clampToPositiveInt(getRawMinOutputCount(priceBonus));
    }

    public int getMaxOutputCount(int priceBonus) {
        if (base == null || max == null) {
            return clampToPositiveInt((long) output.getCount() + priceBonus);
        }

        int min = getMinOutputCount(priceBonus);
        return Math.max(min, clampToPositiveInt((long) max + priceBonus));
    }

    public ItemStack getDisplayOutput(int priceBonus) {
        ItemStack displayOutput = output.copy();
        displayOutput.setCount(Math.max(1, getMaxOutputCount(priceBonus)));
        return displayOutput;
    }

    private static int clampToPositiveInt(long value) { return value <= 0L ? 1 : value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value; }
    private static String normalizeSeasonId(@Nullable String rawSeasonId) { if (rawSeasonId == null || rawSeasonId.isBlank()) return ""; String normalized = rawSeasonId.trim().toLowerCase(Locale.ROOT); return "fall".equals(normalized) ? "autumn" : normalized; }

    public record RecipeInput(List<ItemStack> stacks) implements Container {
        @Override public int getContainerSize() { return stacks.size(); }
        @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
        @Override public ItemStack getItem(int slot) { return stacks.get(slot); }
        @Override public ItemStack removeItem(int slot, int amount) { return ContainerHelper.removeItem(stacks, slot, amount); }
        @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(stacks, slot); }
        @Override public void setItem(int slot, ItemStack stack) { stacks.set(slot, stack); }
        @Override public void setChanged() { }
        @Override public boolean stillValid(Player player) { return true; }
        @Override public void clearContent() { stacks.clear(); }
    }

    public static class Serializer implements RecipeSerializer<SellingBinRecipe> {
        @Override public SellingBinRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            if (!json.has("input") || !json.has("output")) throw new JsonParseException("Invalid selling_bin recipe");
            JsonElement inputElement = json.get("input");
            Ingredient input = Ingredient.fromJson(inputElement);
            int inputCount = inputElement != null && inputElement.isJsonObject() ? GsonHelper.getAsInt(inputElement.getAsJsonObject(), "count", 1) : 1;
            ItemStack output = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "output"), true);
            Integer base = json.has("base") ? GsonHelper.getAsInt(json, "base") : null;
            Integer max = json.has("max") ? GsonHelper.getAsInt(json, "max") : null;
            String group = GsonHelper.getAsString(json, "group", "");
            boolean tradeBalance = GsonHelper.getAsBoolean(json, "trade_balance", false);
            boolean sRegression = GsonHelper.getAsBoolean(json, "s-regression", false);
            String season = normalizeSeasonId(GsonHelper.getAsString(json, "season", ""));
            Integer seasonBase = json.has("season_base") ? Math.max(0, GsonHelper.getAsInt(json, "season_base")) : null;
            Integer seasonMax = json.has("season_max") ? Math.max(0, GsonHelper.getAsInt(json, "season_max")) : null;
            boolean seasonOnly = GsonHelper.getAsBoolean(json, "season_only", false);
            if ((base == null) != (max == null)) { base = null; max = null; }
            if ((seasonBase == null) != (seasonMax == null)) { seasonBase = null; seasonMax = null; }
            if (season.isEmpty()) { seasonBase = null; seasonMax = null; seasonOnly = false; }
            return new SellingBinRecipe(recipeId, input, inputCount, output, base, max, group, tradeBalance, sRegression, season, seasonBase, seasonMax, seasonOnly);
        }

        @Override public @Nullable SellingBinRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buf) {
            Ingredient input = Ingredient.fromNetwork(buf);
            int inputCount = buf.readVarInt();
            ItemStack output = buf.readItem();
            boolean hasRange = buf.readBoolean();
            Integer base = null, max = null;
            if (hasRange) { base = buf.readVarInt(); max = buf.readVarInt(); }
            String group = buf.readUtf();
            boolean tradeBalance = buf.readBoolean();
            boolean sRegression = buf.readBoolean();
            String season = normalizeSeasonId(buf.readUtf());
            boolean hasSeasonRange = buf.readBoolean();
            Integer seasonBase = null, seasonMax = null;
            if (hasSeasonRange) { seasonBase = buf.readVarInt(); seasonMax = buf.readVarInt(); }
            boolean seasonOnly = buf.readBoolean();
            return new SellingBinRecipe(recipeId, input, inputCount, output, base, max, group, tradeBalance, sRegression, season, seasonBase, seasonMax, seasonOnly);
        }

        @Override public void toNetwork(FriendlyByteBuf buf, SellingBinRecipe recipe) {
            recipe.input.toNetwork(buf);
            buf.writeVarInt(recipe.inputCount);
            buf.writeItem(recipe.output);
            boolean hasRange = recipe.base != null && recipe.max != null;
            buf.writeBoolean(hasRange);
            if (hasRange) { buf.writeVarInt(recipe.base); buf.writeVarInt(recipe.max); }
            buf.writeUtf(recipe.group);
            buf.writeBoolean(recipe.tradeBalance);
            buf.writeBoolean(recipe.sRegression);
            buf.writeUtf(recipe.season);
            boolean hasSeasonRange = recipe.seasonBase != null && recipe.seasonMax != null;
            buf.writeBoolean(hasSeasonRange);
            if (hasSeasonRange) { buf.writeVarInt(recipe.seasonBase); buf.writeVarInt(recipe.seasonMax); }
            buf.writeBoolean(recipe.seasonOnly);
        }
    }
}

