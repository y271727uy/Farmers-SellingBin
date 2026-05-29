package com.y271727uy.sellingbin.api.economic;

import com.y271727uy.sellingbin.economic.CurrencyOperationResult;
import com.y271727uy.sellingbin.integration.sdm_integration.SdmCurrencyHelperBridge;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Public currency API for interacting with SDM Economy.
 */
@SuppressWarnings("unused")
public final class ShopcoreCurrency {
    private ShopcoreCurrency() {
    }

    public static boolean isAvailable() {
        return SdmCurrencyHelperBridge.isAvailable();
    }

    public static CurrencyOperationResult increase(Player player, double amount) {
        return SdmCurrencyHelperBridge.increase(player, amount);
    }

    public static CurrencyOperationResult increase(Player player, ItemStack paymentStack) {
        return SdmCurrencyHelperBridge.increase(player, paymentStack);
    }

    public static CurrencyOperationResult increase(UUID accountUuid, double amount) {
        return SdmCurrencyHelperBridge.increase(accountUuid, amount);
    }

    public static CurrencyOperationResult increase(UUID accountUuid, ItemStack paymentStack) {
        return SdmCurrencyHelperBridge.increase(accountUuid, paymentStack);
    }

    public static CurrencyOperationResult decrease(Player player, double amount) {
        return SdmCurrencyHelperBridge.decrease(player, amount);
    }

    public static CurrencyOperationResult decrease(UUID accountUuid, double amount) {
        return SdmCurrencyHelperBridge.decrease(accountUuid, amount);
    }

    public static CurrencyOperationResult adjust(Player player, double delta) {
        return SdmCurrencyHelperBridge.adjust(player, delta);
    }

    public static CurrencyOperationResult adjust(UUID accountUuid, double delta) {
        return SdmCurrencyHelperBridge.adjust(accountUuid, delta);
    }

    public static CurrencyOperationResult settle(Player player, ItemStack paymentStack) {
        return SdmCurrencyHelperBridge.settle(player, paymentStack);
    }

    public static CurrencyOperationResult settle(UUID accountUuid, ItemStack paymentStack) {
        return SdmCurrencyHelperBridge.settle(accountUuid, paymentStack);
    }

    public static CurrencyOperationResult settleVerified(Player player, ItemStack paymentStack) {
        return SdmCurrencyHelperBridge.settleVerified(player, paymentStack);
    }

    public static CurrencyOperationResult settleVerified(UUID accountUuid, ItemStack paymentStack) {
        return SdmCurrencyHelperBridge.settleVerified(accountUuid, paymentStack);
    }

    public static OptionalDouble balance(Player player) {
        return SdmCurrencyHelperBridge.queryBalance(player);
    }

    public static OptionalDouble balance(UUID accountUuid) {
        return SdmCurrencyHelperBridge.queryBalance(accountUuid);
    }
}
