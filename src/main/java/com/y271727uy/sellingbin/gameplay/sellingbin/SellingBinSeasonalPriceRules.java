package com.y271727uy.sellingbin.gameplay.sellingbin;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@SuppressWarnings("unused")
public final class SellingBinSeasonalPriceRules {
    private static final List<SeasonalPriceRule> RULES = new ArrayList<>();
    private static final Comparator<SeasonalPriceRule> RULE_ORDER = Comparator
            .comparing(SeasonalPriceRule::seasonId)
            .thenComparing(SeasonalPriceRule::groupName)
            .thenComparingInt(SeasonalPriceRule::count)
            .thenComparingInt(SeasonalPriceRule::minBonus)
            .thenComparingInt(SeasonalPriceRule::maxBonus);
    private static boolean defaultsRegistered;

    private SellingBinSeasonalPriceRules() {
    }

    public static void clear() {
        RULES.clear();
        defaultsRegistered = false;
    }

    public static SeasonalPriceRule register(SeasonalPriceRule rule) {
        Objects.requireNonNull(rule, "rule");
        RULES.add(rule);
        RULES.sort(RULE_ORDER);
        return rule;
    }

    public static RuleBuilder rule(String seasonId, String groupName) {
        return new RuleBuilder(seasonId, groupName);
    }

    public static RuleBuilder spring(String groupName) {
        return rule("spring", groupName);
    }

    public static void registerDefaults() {
        if (defaultsRegistered) {
            return;
        }

        defaultsRegistered = true;
        winterCrop().count(Integer.MAX_VALUE).bonus(1).register();
    }

    public static RuleBuilder winterCrop() {
        return winter("crop");
    }

    public static SeasonalPriceRule winterCrop(int count, int minBonus, int maxBonus) {
        return winterCrop().count(count).bonus(minBonus, maxBonus).register();
    }

    public static RuleBuilder summer(String groupName) {
        return rule("summer", groupName);
    }

    public static RuleBuilder autumn(String groupName) {
        return rule("autumn", groupName);
    }

    public static RuleBuilder winter(String groupName) {
        return rule("winter", groupName);
    }

    public static List<SeasonalPriceRule> rules() {
        return List.copyOf(RULES);
    }

    private static String normalizeSeasonId(String seasonId) {
        String normalized = Objects.requireNonNull(seasonId, "seasonId").trim().toLowerCase(Locale.ROOT);
        if ("fall".equals(normalized)) {
            return "autumn";
        }
        return normalized;
    }

    private static String normalizeGroupName(String groupName) {
        return Objects.requireNonNull(groupName, "groupName").trim();
    }

    public static final class RuleBuilder {
        private final String seasonId;
        private final String groupName;
        private int count = 1;
        private int minBonus = 1;
        private int maxBonus = 1;

        private RuleBuilder(String seasonId, String groupName) {
            this.seasonId = normalizeSeasonId(seasonId);
            this.groupName = normalizeGroupName(groupName);
        }

        public RuleBuilder count(int count) {
            this.count = Math.max(0, count);
            return this;
        }

        public RuleBuilder bonus(int bonus) {
            return bonus(bonus, bonus);
        }

        public RuleBuilder bonus(int minBonus, int maxBonus) {
            this.minBonus = Math.max(0, minBonus);
            this.maxBonus = Math.max(this.minBonus, Math.max(0, maxBonus));
            return this;
        }

        public SeasonalPriceRule register() {
            return SellingBinSeasonalPriceRules.register(new SeasonalPriceRule(seasonId, groupName, count, minBonus, maxBonus));
        }
    }

    public record SeasonalPriceRule(String seasonId, String groupName, int count, int minBonus, int maxBonus) {
        public SeasonalPriceRule {
            seasonId = normalizeSeasonId(seasonId);
            groupName = normalizeGroupName(groupName);
            count = Math.max(0, count);
            minBonus = Math.max(0, minBonus);
            maxBonus = Math.max(minBonus, Math.max(0, maxBonus));
        }

        public boolean matchesSeason(String currentSeasonId) {
            if (currentSeasonId == null || currentSeasonId.isBlank()) {
                return false;
            }

            return seasonId.equalsIgnoreCase(normalizeSeasonId(currentSeasonId));
        }

        public int bonusFor(ResourceLocation priceKey) {
            if (maxBonus <= minBonus) {
                return minBonus;
            }

            int range = maxBonus - minBonus + 1;
            int seed = Objects.hash(seasonId, groupName, count, minBonus, maxBonus, priceKey.toString());
            return minBonus + Math.floorMod(seed, range);
        }
    }
}

