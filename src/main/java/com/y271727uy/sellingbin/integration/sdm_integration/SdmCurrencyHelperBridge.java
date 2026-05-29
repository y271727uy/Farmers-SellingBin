package com.y271727uy.sellingbin.integration.sdm_integration;

import com.y271727uy.sellingbin.economic.CurrencyDenomination;
import com.y271727uy.sellingbin.economic.CurrencyOperationResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.sixik.sdmshoprework.SDMShopR;

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.UUID;

/**
 * Direct adapter around {@code net.sixik.sdmshoprework.SDMShopR}.
 */
@SuppressWarnings("unused")
public final class SdmCurrencyHelperBridge {
    private SdmCurrencyHelperBridge() {
    }

    public static boolean isAvailable() {
        return true;
    }

    public static CurrencyOperationResult increase(Player player, double amount) {
        return adjust(player, player == null ? null : player.getUUID(), Math.abs(amount));
    }

    public static CurrencyOperationResult increase(Player player, ItemStack paymentStack) {
        return settle(player, player == null ? null : player.getUUID(), paymentStack);
    }

    public static CurrencyOperationResult increase(UUID accountUuid, double amount) {
        return adjust(null, Objects.requireNonNull(accountUuid, "accountUuid"), Math.abs(amount));
    }

    public static CurrencyOperationResult increase(UUID accountUuid, ItemStack paymentStack) {
        return settle(null, Objects.requireNonNull(accountUuid, "accountUuid"), paymentStack);
    }

    public static CurrencyOperationResult decrease(Player player, double amount) {
        return adjust(player, player == null ? null : player.getUUID(), -Math.abs(amount));
    }

    public static CurrencyOperationResult decrease(UUID accountUuid, double amount) {
        return adjust(null, Objects.requireNonNull(accountUuid, "accountUuid"), -Math.abs(amount));
    }

    public static CurrencyOperationResult adjust(Player player, double delta) {
        return adjust(player, player == null ? null : player.getUUID(), delta);
    }

    public static CurrencyOperationResult adjust(UUID accountUuid, double delta) {
        return adjust(null, Objects.requireNonNull(accountUuid, "accountUuid"), delta);
    }

    public static CurrencyOperationResult settle(Player player, ItemStack paymentStack) {
        return settle(player, player == null ? null : player.getUUID(), paymentStack);
    }

    public static CurrencyOperationResult settle(UUID accountUuid, ItemStack paymentStack) {
        return settle(null, Objects.requireNonNull(accountUuid, "accountUuid"), paymentStack);
    }

    public static CurrencyOperationResult settleVerified(Player player, ItemStack paymentStack) {
        return settleVerified(player, player == null ? null : player.getUUID(), paymentStack);
    }

    public static CurrencyOperationResult settleVerified(UUID accountUuid, ItemStack paymentStack) {
        return settleVerified(null, Objects.requireNonNull(accountUuid, "accountUuid"), paymentStack);
    }

    public static OptionalDouble queryBalance(Player player) {
        if (player == null) {
            return OptionalDouble.empty();
        }

        try {
            return OptionalDouble.of(readBalance(player));
        } catch (RuntimeException exception) {
            return OptionalDouble.empty();
        }
    }

    public static OptionalDouble queryBalance(UUID accountUuid) {
        return queryBalance(null, Objects.requireNonNull(accountUuid, "accountUuid"));
    }

    private static CurrencyOperationResult settle(Player player, UUID accountUuid, ItemStack paymentStack) {
        if (paymentStack == null || paymentStack.isEmpty()) {
            return CurrencyOperationResult.failure(0D, "payment stack is empty");
        }

        var denomination = CurrencyDenomination.fromItemStack(paymentStack);
        if (denomination.isEmpty()) {
            return CurrencyOperationResult.failure(0D, "unsupported currency item: " + paymentStack.getItem());
        }

        double amount = (double) denomination.get().totalValue(paymentStack.getCount());
        return adjust(player, accountUuid, amount);
    }

    private static CurrencyOperationResult settleVerified(Player player, UUID accountUuid, ItemStack paymentStack) {
        if (paymentStack == null || paymentStack.isEmpty()) {
            return CurrencyOperationResult.failure(0D, "payment stack is empty");
        }

        OptionalDouble before = queryBalance(player, accountUuid);
        CurrencyOperationResult result = settle(player, accountUuid, paymentStack);
        if (!result.success()) {
            return result;
        }

        OptionalDouble after = queryBalance(player, accountUuid);
        if (before.isPresent() && after.isPresent() && after.getAsDouble() <= before.getAsDouble()) {
            return CurrencyOperationResult.failure(result.delta(), "balance did not increase after settlement");
        }

        return result;
    }

    private static CurrencyOperationResult adjust(Player player, UUID accountUuid, double delta) {
        if (!Double.isFinite(delta)) {
            return CurrencyOperationResult.failure(delta, "currency amount must be finite");
        }

        Player target = resolvePlayer(player, accountUuid);
        if (target == null) {
            return CurrencyOperationResult.failure(delta, "player is not online and no direct SDM account bridge is available");
        }

        if (delta == 0D) {
            return CurrencyOperationResult.success(0D, queryBalanceResolved(target), "no-op currency change");
        }

        try {
            double current = readBalance(target);
            double updated = current + delta;
            if (updated < 0D) {
                return CurrencyOperationResult.failure(delta, "currency balance would become negative");
            }

            writeBalance(target, updated);

            OptionalDouble after = queryBalanceResolved(target);
            if (after.isPresent() && Math.abs(after.getAsDouble() - updated) > 0.0001D) {
                return CurrencyOperationResult.failure(delta, "SDM balance verification failed after update");
            }

            return CurrencyOperationResult.success(delta, after, "SDMShopR balance updated successfully");
        } catch (RuntimeException exception) {
            return CurrencyOperationResult.failure(delta, exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }

    private static OptionalDouble queryBalance(Player player, UUID accountUuid) {
        Player target = resolvePlayer(player, accountUuid);
        if (target == null) {
            return OptionalDouble.empty();
        }
        return queryBalanceResolved(target);
    }

    private static OptionalDouble queryBalanceResolved(Player player) {
        return queryBalance(player);
    }

    private static Player resolvePlayer(Player player, UUID accountUuid) {
        if (player != null) {
            return player;
        }
        if (accountUuid == null) {
            return null;
        }

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }

        return server.getPlayerList().getPlayer(accountUuid);
    }

    private static double readBalance(Player player) {
        return SDMShopR.getMoney(player);
    }

    private static void writeBalance(Player player, double amount) {
        SDMShopR.setMoney(player, (int) Math.round(amount));
    }
}

