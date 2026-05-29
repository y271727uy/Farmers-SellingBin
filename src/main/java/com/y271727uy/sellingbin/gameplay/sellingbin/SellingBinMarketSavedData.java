package com.y271727uy.sellingbin.gameplay.sellingbin;

import com.y271727uy.sellingbin.SellingBinMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unused", "all"})
public final class SellingBinMarketSavedData extends SavedData {
    private static final String DATA_NAME = SellingBinMod.MODID + "_selling_bin_market";
    private static final String FLOATING_BONUSES_TAG = "PriceBonuses";
    private static final String VIRTUAL_STOCK_TAG = "VirtualStock";
    private static final String SEASONAL_BONUSES_TAG = "SeasonalPriceBonuses";
    private static final String LONG_TERM_BONUSES_TAG = "LongTermPriceBonuses";
    private static final String LONG_TERM_PRICE_KEY_TAG = "PriceKey";
    private static final String LONG_TERM_LAST_SELL_DAY_TAG = "LastSellDay";
    private static final String LONG_TERM_LEGACY_R_TAG = "LegacyR";
    private static final long UNINITIALIZED_DAY = Long.MIN_VALUE;
    private static final int MAX_VIRTUAL_STOCK = 401;
    private static final int NEAR_ZERO_CUTOFF = 3;
    private static final int MAX_LONG_TERM_R = 7;

    private final Map<ResourceLocation, Integer> floatingPriceBonusByRecipe = new HashMap<>();
    private final Map<ResourceLocation, Integer> virtualStockByItem = new HashMap<>();
    private final Map<ResourceLocation, Integer> seasonalPriceBonusByRecipe = new HashMap<>();
    private final Map<ResourceLocation, Integer> carryStageByRecipe = new HashMap<>();
    private final Map<ResourceLocation, Long> lastSellDayByPriceKey = new HashMap<>();
    private final Map<ResourceLocation, Integer> legacyRByPriceKey = new HashMap<>();
    private long lastProcessedDay = UNINITIALIZED_DAY;

    @SuppressWarnings("resource")
    static SellingBinMarketSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(SellingBinMarketSavedData::load, SellingBinMarketSavedData::new, DATA_NAME);
    }

    static SellingBinMarketSavedData load(CompoundTag tag) {
        SellingBinMarketSavedData data = new SellingBinMarketSavedData();
        if (tag.contains("LastProcessedDay")) {
            data.lastProcessedDay = tag.getLong("LastProcessedDay");
        }
        loadBonusList(tag.getList(FLOATING_BONUSES_TAG, Tag.TAG_COMPOUND), data.floatingPriceBonusByRecipe, data.carryStageByRecipe);
        loadStockList(tag.getList(VIRTUAL_STOCK_TAG, Tag.TAG_COMPOUND), data.virtualStockByItem);
        loadBonusList(tag.getList(SEASONAL_BONUSES_TAG, Tag.TAG_COMPOUND), data.seasonalPriceBonusByRecipe, null);
        loadLongTermBonusList(tag.getList(LONG_TERM_BONUSES_TAG, Tag.TAG_COMPOUND), data.lastSellDayByPriceKey, data.legacyRByPriceKey);
        return data;
    }

    private static void loadStockList(ListTag bonuses, Map<ResourceLocation, Integer> targetStocks) {
        for (Tag element : bonuses) {
            CompoundTag bonusTag = (CompoundTag) element;
            if (!bonusTag.contains("Item", Tag.TAG_STRING)) {
                continue;
            }

            ResourceLocation itemId = ResourceLocation.tryParse(bonusTag.getString("Item"));
            if (itemId == null) {
                continue;
            }

            int stock = Math.max(0, bonusTag.getInt("Stock"));
            if (stock == 0) {
                continue;
            }

            targetStocks.put(itemId, stock);
        }
    }

    private static void loadBonusList(ListTag bonuses, Map<ResourceLocation, Integer> targetBonuses, Map<ResourceLocation, Integer> carryStages) {
        for (Tag element : bonuses) {
            CompoundTag bonusTag = (CompoundTag) element;
            if (!bonusTag.contains("Recipe", Tag.TAG_STRING)) {
                continue;
            }

            ResourceLocation recipeId = ResourceLocation.tryParse(bonusTag.getString("Recipe"));
            if (recipeId == null) {
                continue;
            }

            int bonus = bonusTag.getInt("Bonus");
            if (bonus == 0) {
                continue;
            }

            targetBonuses.put(recipeId, bonus);
            if (carryStages != null) {
                carryStages.put(recipeId, Math.max(0, bonusTag.getInt("CarryStage")));
            }
        }
    }

    private static void loadLongTermBonusList(ListTag bonuses, Map<ResourceLocation, Long> lastSellDays, Map<ResourceLocation, Integer> legacyBonuses) {
        for (Tag element : bonuses) {
            CompoundTag bonusTag = (CompoundTag) element;
            if (!bonusTag.contains(LONG_TERM_PRICE_KEY_TAG, Tag.TAG_STRING)) {
                continue;
            }

            ResourceLocation priceKey = ResourceLocation.tryParse(bonusTag.getString(LONG_TERM_PRICE_KEY_TAG));
            if (priceKey == null || !bonusTag.contains(LONG_TERM_LAST_SELL_DAY_TAG, Tag.TAG_LONG)) {
                continue;
            }

            long lastSellDay = bonusTag.getLong(LONG_TERM_LAST_SELL_DAY_TAG);
            if (lastSellDay != UNINITIALIZED_DAY) {
                lastSellDays.put(priceKey, lastSellDay);
                if (legacyBonuses != null) {
                    int legacyR = clampLongTermR(bonusTag.contains(LONG_TERM_LEGACY_R_TAG, Tag.TAG_INT) ? bonusTag.getInt(LONG_TERM_LEGACY_R_TAG) : 0);
                    if (legacyR > 0) {
                        legacyBonuses.put(priceKey, legacyR);
                    }
                }
            }
        }
    }

    boolean isInitialized() { return lastProcessedDay != UNINITIALIZED_DAY; }
    long getLastProcessedDay() { return lastProcessedDay; }
    void setLastProcessedDay(long dayIndex) { if (lastProcessedDay != dayIndex) { lastProcessedDay = dayIndex; setDirty(); } }

    int getPriceBonus(ResourceLocation recipeId, long currentDay) { return clampToInt((long) getFloatingPriceBonus(recipeId) + getVirtualStockPriceBonus(recipeId) + getSeasonalPriceBonus(recipeId) + getLongTermPriceBonus(recipeId, currentDay)); }
    int getFloatingPriceBonus(ResourceLocation recipeId) { return floatingPriceBonusByRecipe.getOrDefault(recipeId, 0); }
    int getVirtualStockPriceBonus(ResourceLocation itemId) { return getVirtualStockPriceBonus(getVirtualStock(itemId)); }
    int getSeasonalPriceBonus(ResourceLocation recipeId) { return seasonalPriceBonusByRecipe.getOrDefault(recipeId, 0); }
    int getLongTermPriceBonus(ResourceLocation priceKey, long currentDay) { return getLongTermPriceBonusForR(getLongTermRarityValue(priceKey, currentDay)); }

    boolean setFloatingPriceBonus(ResourceLocation recipeId, int bonus) { if (getFloatingPriceBonus(recipeId) == bonus) return false; if (bonus == 0) { floatingPriceBonusByRecipe.remove(recipeId); carryStageByRecipe.remove(recipeId); } else { floatingPriceBonusByRecipe.put(recipeId, bonus); } setDirty(); return true; }
    int getVirtualStock(ResourceLocation itemId) { return virtualStockByItem.getOrDefault(itemId, 0); }
    boolean addVirtualStock(ResourceLocation itemId, int amount) { if (amount == 0) return false; int nextStock = normalizeVirtualStock((long) getVirtualStock(itemId) + amount); if (nextStock == getVirtualStock(itemId)) return false; if (nextStock == 0) virtualStockByItem.remove(itemId); else virtualStockByItem.put(itemId, nextStock); setDirty(); return true; }
    boolean recordSale(ResourceLocation priceKey, long currentDay) {
        boolean changed = false;

        long previousLastSellDay = lastSellDayByPriceKey.getOrDefault(priceKey, UNINITIALIZED_DAY);
        if (previousLastSellDay != currentDay) {
            lastSellDayByPriceKey.put(priceKey, currentDay);
            changed = true;
        }

        if (previousLastSellDay != UNINITIALIZED_DAY) {
            long rawR = Math.max(0L, currentDay - previousLastSellDay);
            if (rawR > 0L) {
                int currentLegacyR = getLegacyR(priceKey);
                int nextLegacyR = clampLongTermR((long) currentLegacyR + rawR);
                if (currentLegacyR != nextLegacyR) {
                    legacyRByPriceKey.put(priceKey, nextLegacyR);
                    changed = true;
                }
            }
        }

        if (changed) {
            setDirty();
        }

        return changed;
    }
    boolean clearFloatingAdjustments() { if (floatingPriceBonusByRecipe.isEmpty() && carryStageByRecipe.isEmpty()) return false; floatingPriceBonusByRecipe.clear(); carryStageByRecipe.clear(); setDirty(); return true; }
    boolean setSeasonalPriceBonuses(Map<ResourceLocation, Integer> bonuses) { if (seasonalPriceBonusByRecipe.equals(bonuses)) return false; seasonalPriceBonusByRecipe.clear(); seasonalPriceBonusByRecipe.putAll(bonuses); setDirty(); return true; }
    boolean setCarryStage(ResourceLocation recipeId, int carryStage) { int normalized = Math.max(0, carryStage); if (!floatingPriceBonusByRecipe.containsKey(recipeId)) normalized = 0; if (carryStageByRecipe.getOrDefault(recipeId, 0) == normalized) return false; if (normalized == 0) carryStageByRecipe.remove(recipeId); else carryStageByRecipe.put(recipeId, normalized); setDirty(); return true; }
    int getCarryStage(ResourceLocation recipeId) { return Math.max(0, carryStageByRecipe.getOrDefault(recipeId, 0)); }
    boolean applyDailyDecay(long currentDay, Map<ResourceLocation, Boolean> sRegressionByPriceKey) {
        if (virtualStockByItem.isEmpty() && legacyRByPriceKey.isEmpty()) {
            return false;
        }

        boolean virtualChanged = false;
        Map<ResourceLocation, Integer> nextStocks = new HashMap<>(virtualStockByItem.size());
        for (Map.Entry<ResourceLocation, Integer> entry : virtualStockByItem.entrySet()) {
            int currentStock = normalizeVirtualStock(entry.getValue());
            boolean sRegression = sRegressionByPriceKey.getOrDefault(entry.getKey(), false);
            int nextStock = decayVirtualStock(currentStock, sRegression);
            if (nextStock > 0) {
                nextStocks.put(entry.getKey(), nextStock);
            }
            if (nextStock != currentStock) {
                virtualChanged = true;
            }
        }

        boolean legacyChanged = false;
        Map<ResourceLocation, Integer> nextLegacyBonuses = new HashMap<>(legacyRByPriceKey.size());
        for (Map.Entry<ResourceLocation, Integer> entry : legacyRByPriceKey.entrySet()) {
            int currentLegacyR = clampLongTermR(entry.getValue());
            int nextLegacyR = Math.max(0, currentLegacyR - 1);
            if (nextLegacyR > 0) {
                nextLegacyBonuses.put(entry.getKey(), nextLegacyR);
            } else if (currentLegacyR > 0 && lastSellDayByPriceKey.containsKey(entry.getKey())) {
                lastSellDayByPriceKey.put(entry.getKey(), currentDay);
                legacyChanged = true;
            }
            if (nextLegacyR != currentLegacyR) {
                legacyChanged = true;
            }
        }

        if (!virtualChanged && !legacyChanged) {
            return false;
        }

        virtualStockByItem.clear();
        virtualStockByItem.putAll(nextStocks);
        legacyRByPriceKey.clear();
        legacyRByPriceKey.putAll(nextLegacyBonuses);
        setDirty();
        return true;
    }

    Map<ResourceLocation, Integer> snapshotFloatingPriceBonuses() { return new HashMap<>(floatingPriceBonusByRecipe); }
    Map<ResourceLocation, Integer> snapshotVirtualStockPriceBonuses() { Map<ResourceLocation, Integer> virtualBonuses = new HashMap<>(); virtualStockByItem.forEach((itemId, stock) -> { int bonus = getVirtualStockPriceBonus(stock); if (bonus != 0) virtualBonuses.put(itemId, bonus); }); return virtualBonuses; }
    Map<ResourceLocation, Integer> snapshotSeasonalPriceBonuses() { return new HashMap<>(seasonalPriceBonusByRecipe); }
    public Map<ResourceLocation, Integer> snapshotCarryStages() { return new HashMap<>(carryStageByRecipe); }
    Map<ResourceLocation, Integer> snapshotLongTermPriceBonuses(long currentDay) { Map<ResourceLocation, Integer> longTermBonuses = new HashMap<>(); for (ResourceLocation priceKey : lastSellDayByPriceKey.keySet()) { int bonus = getLongTermPriceBonus(priceKey, currentDay); if (bonus != 0) longTermBonuses.put(priceKey, bonus); } return longTermBonuses; }
    Map<ResourceLocation, Integer> snapshotPriceBonuses(long currentDay) { Map<ResourceLocation, Integer> totalBonuses = snapshotSeasonalPriceBonuses(); snapshotFloatingPriceBonuses().forEach((recipeId, bonus) -> totalBonuses.merge(recipeId, bonus, SellingBinMarketSavedData::mergeBonus)); snapshotVirtualStockPriceBonuses().forEach((itemId, bonus) -> totalBonuses.merge(itemId, bonus, SellingBinMarketSavedData::mergeBonus)); snapshotLongTermPriceBonuses(currentDay).forEach((priceKey, bonus) -> totalBonuses.merge(priceKey, bonus, SellingBinMarketSavedData::mergeBonus)); totalBonuses.entrySet().removeIf(entry -> entry.getValue() == 0); return totalBonuses; }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("LastProcessedDay", lastProcessedDay);
        ListTag floatingBonuses = new ListTag();
        floatingPriceBonusByRecipe.forEach((recipeId, bonus) -> { if (bonus != 0) { CompoundTag bonusTag = new CompoundTag(); bonusTag.putString("Recipe", recipeId.toString()); bonusTag.putInt("Bonus", bonus); bonusTag.putInt("CarryStage", getCarryStage(recipeId)); floatingBonuses.add(bonusTag); } });
        tag.put(FLOATING_BONUSES_TAG, floatingBonuses);
        ListTag virtualStocks = new ListTag();
        virtualStockByItem.forEach((itemId, stock) -> { if (stock > 0) { CompoundTag stockTag = new CompoundTag(); stockTag.putString("Item", itemId.toString()); stockTag.putInt("Stock", stock); virtualStocks.add(stockTag); } });
        tag.put(VIRTUAL_STOCK_TAG, virtualStocks);
        ListTag seasonalBonuses = new ListTag();
        seasonalPriceBonusByRecipe.forEach((recipeId, bonus) -> { if (bonus != 0) { CompoundTag bonusTag = new CompoundTag(); bonusTag.putString("Recipe", recipeId.toString()); bonusTag.putInt("Bonus", bonus); seasonalBonuses.add(bonusTag); } });
        tag.put(SEASONAL_BONUSES_TAG, seasonalBonuses);
        ListTag longTermBonuses = new ListTag();
        lastSellDayByPriceKey.forEach((priceKey, lastSellDay) -> { CompoundTag bonusTag = new CompoundTag(); bonusTag.putString(LONG_TERM_PRICE_KEY_TAG, priceKey.toString()); bonusTag.putLong(LONG_TERM_LAST_SELL_DAY_TAG, lastSellDay); int legacyR = legacyRByPriceKey.getOrDefault(priceKey, 0); if (legacyR > 0) bonusTag.putInt(LONG_TERM_LEGACY_R_TAG, legacyR); longTermBonuses.add(bonusTag); });
        tag.put(LONG_TERM_BONUSES_TAG, longTermBonuses);
        return tag;
    }

    private static int mergeBonus(int left, int right) { long sum = (long) left + right; if (sum <= Integer.MIN_VALUE) return Integer.MIN_VALUE; if (sum >= Integer.MAX_VALUE) return Integer.MAX_VALUE; return (int) sum; }
    private static int getVirtualStockPriceBonus(int stock) { if (stock > 400) return -15; if (stock > 300) return -9; if (stock > 200) return -7; if (stock > 100) return -5; if (stock > 50) return -1; return 0; }
    static int decayVirtualStock(int stock, boolean sRegression) { if (stock <= NEAR_ZERO_CUTOFF) return 0; int decayPercent; if (sRegression) { if (stock >= 300) decayPercent = 35; else if (stock >= 200) decayPercent = 20; else if (stock >= 100) decayPercent = 10; else decayPercent = 5; } else { if (stock >= 300) decayPercent = 5; else if (stock >= 200) decayPercent = 10; else if (stock >= 100) decayPercent = 20; else decayPercent = 35; } int nextStock = (stock * (100 - decayPercent)) / 100; if (nextStock >= stock) return 0; return Math.max(0, nextStock); }
    private int getLongTermRarityValue(ResourceLocation priceKey, long currentDay) { Long lastSellDay = lastSellDayByPriceKey.get(priceKey); if (lastSellDay == null || lastSellDay == UNINITIALIZED_DAY) return 0; return (int) Math.min(7L, Math.max(0L, currentDay - lastSellDay)); }
    private int getLegacyR(ResourceLocation priceKey) { return clampLongTermR(legacyRByPriceKey.getOrDefault(priceKey, 0)); }
    private static int getLongTermPriceBonusForR(int rarityValue) { return switch (clampLongTermR(rarityValue)) { case 0, 1, 2 -> 0; case 3, 4 -> 1; case 5, 6 -> 2; default -> 3; }; }
    private static int clampLongTermR(long rarityValue) { if (rarityValue <= 0L) return 0; return (int) Math.min(MAX_LONG_TERM_R, rarityValue); }
    private static int normalizeVirtualStock(long stock) { return stock <= 0L ? 0 : (int) Math.min(MAX_VIRTUAL_STOCK, stock); }
    private static int clampToInt(long value) { if (value <= Integer.MIN_VALUE) return Integer.MIN_VALUE; if (value >= Integer.MAX_VALUE) return Integer.MAX_VALUE; return (int) value; }
}

