package com.y271727uy.sellingbin.gameplay.sellingbin;

import com.mojang.logging.LogUtils;
import com.y271727uy.sellingbin.all.ModRecipes;
import com.y271727uy.sellingbin.integration.sereneseasons.SereneSeasonsCompat;
import com.y271727uy.sellingbin.recipe.SellingBinRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

@SuppressWarnings("unused")
public final class SellingBinGroupManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final long DAY_LENGTH_TICKS = 24000L;
    private static final int DAILY_INCREASE_PICK_MIN = 1;
    private static final int DAILY_INCREASE_PICK_MAX = 3;
    private static final int DAILY_INCREASE_AMOUNT_MIN = 1;
    private static final int DAILY_INCREASE_AMOUNT_MAX = 4;
    private static final int DAILY_DECREASE_PICK_MIN = 1;
    private static final int DAILY_DECREASE_PICK_MAX = 3;
    private static final int DAILY_DECREASE_AMOUNT_MIN = 1;
    private static final int DAILY_DECREASE_AMOUNT_MAX = 3;
    private static final float FIRST_CARRY_CHANCE = 0.25F;
    private static final float SECOND_CARRY_CHANCE = 0.10F;
    private static final Object GROUP_CACHE_LOCK = new Object();

    private static Map<String, SellingBinGroup> cachedGroups;

    private SellingBinGroupManager() {
    }

    public static int getPriceBonus(ServerLevel level, ResourceLocation recipeId) {
        SellingBinMarketSavedData marketData = SellingBinMarketSavedData.get(level);
        long currentDay = Math.floorDiv(level.getDayTime(), DAY_LENGTH_TICKS);
        return marketData.getPriceBonus(recipeId, currentDay);
    }

    public static Map<ResourceLocation, Integer> snapshotPriceBonuses(ServerLevel level) {
        SellingBinMarketSavedData marketData = SellingBinMarketSavedData.get(level);
        long currentDay = Math.floorDiv(level.getDayTime(), DAY_LENGTH_TICKS);
        return marketData.snapshotPriceBonuses(currentDay);
    }

    public static Map<ResourceLocation, Integer> snapshotFloatingPriceBonuses(ServerLevel level) {
        return SellingBinMarketSavedData.get(level).snapshotFloatingPriceBonuses();
    }

    public static Map<ResourceLocation, Integer> snapshotSeasonalPriceBonuses(ServerLevel level) {
        return SellingBinMarketSavedData.get(level).snapshotSeasonalPriceBonuses();
    }

    public static Map<ResourceLocation, Integer> snapshotVirtualStockPriceBonuses(ServerLevel level) {
        return SellingBinMarketSavedData.get(level).snapshotVirtualStockPriceBonuses();
    }

    public static Map<ResourceLocation, Integer> snapshotLongTermPriceBonuses(ServerLevel level) {
        SellingBinMarketSavedData marketData = SellingBinMarketSavedData.get(level);
        long currentDay = Math.floorDiv(level.getDayTime(), DAY_LENGTH_TICKS);
        return marketData.snapshotLongTermPriceBonuses(currentDay);
    }

    public static boolean recordSale(ServerLevel level, SellingBinRecipe recipe, ItemStack soldStack, int soldCount) {
        if (!recipe.isTradeBalance() || soldStack.isEmpty() || soldCount <= 0) {
            return false;
        }

        SellingBinMarketSavedData marketData = SellingBinMarketSavedData.get(level);
        ResourceLocation priceKey = recipe.getPriceKey(soldStack);
        long currentDay = Math.floorDiv(level.getDayTime(), DAY_LENGTH_TICKS);
        boolean changed = marketData.recordSale(priceKey, currentDay);
        changed |= marketData.addVirtualStock(priceKey, soldCount);
        return changed;
    }

    public static void invalidateCachedGroups() {
        synchronized (GROUP_CACHE_LOCK) {
            cachedGroups = null;
        }
    }

    public static boolean refreshForElapsedDays(ServerLevel level) {
        SellingBinMarketSavedData marketData = SellingBinMarketSavedData.get(level);
        long currentDay = Math.floorDiv(level.getDayTime(), DAY_LENGTH_TICKS);
        Map<String, SellingBinGroup> groups = collectGroups(level);
        Map<ResourceLocation, Boolean> sRegressionByPriceKey = collectSRegressionModes(level);
        String currentSeasonId = SereneSeasonsCompat.getCurrentSeasonId(level).orElse("");

        if (!marketData.isInitialized()) {
            marketData.setLastProcessedDay(currentDay - 1L);
        }

        boolean updatedPrices = refreshSeasonalAdjustments(level, marketData, currentSeasonId);

        long lastProcessedDay = marketData.getLastProcessedDay();
        if (currentDay <= lastProcessedDay) {
            return updatedPrices;
        }

        for (long day = lastProcessedDay + 1; day <= currentDay; day++) {
            Map<ResourceLocation, Integer> previousFloatingBonuses = marketData.snapshotFloatingPriceBonuses();
            Map<ResourceLocation, Integer> previousCarryStages = marketData.snapshotCarryStages();
            updatedPrices |= marketData.clearFloatingAdjustments();
            for (SellingBinGroup group : groups.values()) {
                updatedPrices |= applyDailyGroupAdjustments(level, day, marketData, group, previousFloatingBonuses, previousCarryStages);
            }
            updatedPrices |= marketData.applyDailyDecay(day, sRegressionByPriceKey);
        }

        marketData.setLastProcessedDay(currentDay);
        return updatedPrices;
    }

    public static Map<String, SellingBinGroup> collectGroups(ServerLevel level) {
        synchronized (GROUP_CACHE_LOCK) {
            if (cachedGroups == null) {
                cachedGroups = buildGroups(level);
            }
            return Collections.unmodifiableMap(cachedGroups);
        }
    }

    private static Map<String, SellingBinGroup> buildGroups(ServerLevel level) {
        Map<String, SellingBinGroup> groups = new LinkedHashMap<>();
        for (SellingBinRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.SELLING_BIN_RECIPE_TYPE.get())) {
            String groupName = recipe.getGroup().trim();
            if (groupName.isEmpty()) {
                continue;
            }

            groups.computeIfAbsent(groupName, ignored -> new SellingBinGroup()).addRecipe(recipe);
        }

        groups.values().removeIf(SellingBinGroup::isEmpty);
        return groups;
    }

    private static Map<ResourceLocation, Boolean> collectSRegressionModes(ServerLevel level) {
        Map<ResourceLocation, Boolean> sRegressionByPriceKey = new HashMap<>();
        Set<ResourceLocation> warnedPriceKeys = new java.util.HashSet<>();

        for (SellingBinRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.SELLING_BIN_RECIPE_TYPE.get())) {
            if (!recipe.isTradeBalance()) {
                continue;
            }

            for (ResourceLocation priceKey : recipe.getPriceKeys()) {
                Boolean previousValue = sRegressionByPriceKey.putIfAbsent(priceKey, recipe.isSRegression());
                if (previousValue == null || previousValue == recipe.isSRegression()) {
                    continue;
                }

                sRegressionByPriceKey.put(priceKey, false);
                if (warnedPriceKeys.add(priceKey)) {
                    LOGGER.warn("Conflicting 's-regression' values detected for selling-bin price key {}. Falling back to false.", priceKey);
                }
            }
        }

        return sRegressionByPriceKey;
    }

    private static boolean refreshSeasonalAdjustments(ServerLevel level, SellingBinMarketSavedData marketData, String currentSeasonId) {
        return marketData.setSeasonalPriceBonuses(buildSeasonalBonuses(level, currentSeasonId));
    }

    private static Map<ResourceLocation, Integer> buildSeasonalBonuses(ServerLevel level, String currentSeasonId) {
        Map<ResourceLocation, Integer> seasonalBonuses = new HashMap<>();
        if (currentSeasonId == null || currentSeasonId.isBlank()) {
            return seasonalBonuses;
        }

        for (SellingBinRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipes.SELLING_BIN_RECIPE_TYPE.get())) {
            if (!recipe.matchesSeason(currentSeasonId)) {
                continue;
            }

            for (ResourceLocation priceKey : recipe.getPriceKeys()) {
                int bonus = recipe.getConfiguredSeasonalPriceBonus(currentSeasonId, priceKey);
                if (bonus <= 0) {
                    continue;
                }
                seasonalBonuses.merge(priceKey, bonus, SellingBinGroupManager::mergeBonus);
            }
        }

        seasonalBonuses.entrySet().removeIf(entry -> entry.getValue() == 0);
        return seasonalBonuses;
    }

    private static boolean applyDailyGroupAdjustments(
            ServerLevel level,
            long currentDay,
            SellingBinMarketSavedData marketData,
            SellingBinGroup group,
            Map<ResourceLocation, Integer> previousFloatingBonuses,
            Map<ResourceLocation, Integer> previousCarryStages
    ) {
        List<SellingBinGroup.Entry> remainingEntries = group.getEntries();
        if (remainingEntries.isEmpty()) {
            return false;
        }

        shuffleEntries(remainingEntries, level.random);

        boolean changed = false;
        int reservedIncreaseSlots = 0;
        int reservedDecreaseSlots = 0;
        for (int i = remainingEntries.size() - 1; i >= 0; i--) {
            SellingBinGroup.Entry entry = remainingEntries.get(i);
            SellingBinRecipe recipe = entry.recipe();
            ResourceLocation priceKey = entry.priceKey();
            int previousBonus = previousFloatingBonuses.getOrDefault(priceKey, 0);
            if (previousBonus == 0) {
                continue;
            }

            int seasonalBonus = marketData.getSeasonalPriceBonus(priceKey);
            int virtualStockBonus = marketData.getVirtualStockPriceBonus(priceKey);
            int longTermBonus = marketData.getLongTermPriceBonus(priceKey, currentDay);
            Integer nextCarryStage = getNextCarryStage(level.random, previousCarryStages.getOrDefault(priceKey, 0));
            if (nextCarryStage == null) {
                continue;
            }

            long currentTotalBonus = (long) previousBonus + seasonalBonus + virtualStockBonus + longTermBonus;
            if (!isLegalPriceBonus(recipe, clampToInt(currentTotalBonus))) {
                continue;
            }

            changed |= applyActivePrice(marketData, priceKey, previousBonus, nextCarryStage);
            remainingEntries.remove(i);
            if (previousBonus > 0) {
                reservedIncreaseSlots++;
            } else {
                reservedDecreaseSlots++;
            }
        }

        int targetIncreaseCount = getRandomInRange(level.random, DAILY_INCREASE_PICK_MIN, DAILY_INCREASE_PICK_MAX);
        int targetDecreaseCount = getRandomInRange(level.random, DAILY_DECREASE_PICK_MIN, DAILY_DECREASE_PICK_MAX);
        int requestedFreshIncreaseCount = Math.max(0, targetIncreaseCount - reservedIncreaseSlots);
        int requestedFreshDecreaseCount = Math.max(0, targetDecreaseCount - reservedDecreaseSlots);

        if (remainingEntries.size() == 1 && requestedFreshIncreaseCount > 0 && requestedFreshDecreaseCount > 0) {
            if (level.random.nextBoolean()) {
                requestedFreshDecreaseCount = 0;
            } else {
                requestedFreshIncreaseCount = 0;
            }
        }

        int increaseCount = Math.min(requestedFreshIncreaseCount, Math.max(0, remainingEntries.size() - requestedFreshDecreaseCount));
        for (int i = 0; i < increaseCount; i++) {
            SellingBinGroup.Entry entry = remainingEntries.remove(remainingEntries.size() - 1);
            int amount = getRandomInRange(level.random, DAILY_INCREASE_AMOUNT_MIN, DAILY_INCREASE_AMOUNT_MAX);
            changed |= applyPriceDelta(marketData, currentDay, entry, amount);
        }

        int decreaseBudget = Math.min(requestedFreshDecreaseCount, remainingEntries.size());
        for (int i = remainingEntries.size() - 1; i >= 0 && decreaseBudget > 0; i--) {
            SellingBinGroup.Entry entry = remainingEntries.get(i);
            int amount = getRandomInRange(level.random, DAILY_DECREASE_AMOUNT_MIN, DAILY_DECREASE_AMOUNT_MAX);
            if (applyPriceDelta(marketData, currentDay, entry, -amount)) {
                changed = true;
                decreaseBudget--;
            }
        }

        return changed;
    }

    private static Integer getNextCarryStage(RandomSource random, int previousCarryStage) {
        if (previousCarryStage <= 0) {
            return random.nextFloat() < FIRST_CARRY_CHANCE ? 1 : null;
        }
        if (previousCarryStage == 1) {
            return random.nextFloat() < SECOND_CARRY_CHANCE ? 2 : null;
        }
        return null;
    }

    private static boolean applyActivePrice(SellingBinMarketSavedData marketData, ResourceLocation priceKey, int priceBonus, int carryStage) {
        boolean changed = marketData.setFloatingPriceBonus(priceKey, priceBonus);
        changed |= marketData.setCarryStage(priceKey, carryStage);
        return changed;
    }

    private static boolean applyPriceDelta(SellingBinMarketSavedData marketData, long currentDay, SellingBinGroup.Entry entry, int delta) {
        if (delta == 0) {
            return false;
        }

        SellingBinRecipe recipe = entry.recipe();
        ResourceLocation priceKey = entry.priceKey();
        int currentFloatingBonus = marketData.getFloatingPriceBonus(priceKey);
        int seasonalBonus = marketData.getSeasonalPriceBonus(priceKey);
        int virtualStockBonus = marketData.getVirtualStockPriceBonus(priceKey);
        int longTermBonus = marketData.getLongTermPriceBonus(priceKey, currentDay);
        long currentTotalBonus = (long) currentFloatingBonus + seasonalBonus + virtualStockBonus + longTermBonus;
        if (delta < 0) {
            Integer legalDelta = getLegalDecreaseDelta(recipe, currentTotalBonus, delta);
            if (legalDelta == null) {
                return false;
            }
            delta = legalDelta;
        }

        long nextBonus = (long) currentFloatingBonus + delta;
        if (nextBonus >= Integer.MAX_VALUE) {
            return applyActivePrice(marketData, priceKey, Integer.MAX_VALUE, 0);
        }

        return applyActivePrice(marketData, priceKey, (int) nextBonus, 0);
    }

    private static Integer getLegalDecreaseDelta(SellingBinRecipe recipe, long currentTotalBonus, int requestedDelta) {
        if (requestedDelta >= 0) {
            return requestedDelta;
        }

        if (isLegalPriceBonus(recipe, clampToInt(currentTotalBonus + requestedDelta))) {
            return requestedDelta;
        }

        if (requestedDelta != -1 && isLegalPriceBonus(recipe, clampToInt(currentTotalBonus - 1L))) {
            return -1;
        }

        return null;
    }

    private static boolean isLegalPriceBonus(SellingBinRecipe recipe, int priceBonus) {
        return recipe.getRawMinOutputCount(priceBonus) >= 1L;
    }

    private static int getRandomInRange(RandomSource random, int min, int max) {
        if (max <= min) {
            return min;
        }

        return min + random.nextInt(max - min + 1);
    }

    private static void shuffleEntries(List<SellingBinGroup.Entry> entries, RandomSource random) {
        for (int i = entries.size() - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            Collections.swap(entries, i, swapIndex);
        }
    }

    private static int mergeBonus(int left, int right) {
        long sum = (long) left + right;
        if (sum <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (sum >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) sum;
    }

    private static int clampToInt(long value) {
        if (value <= Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        if (value >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) value;
    }
}
