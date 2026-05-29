package com.y271727uy.sellingbin.event;

import com.y271727uy.sellingbin.SellingBinMod;
import com.y271727uy.sellingbin.gameplay.sellingbin.SellingBinGroupManager;
import com.y271727uy.sellingbin.network.ModMessages;
import com.y271727uy.sellingbin.network.SellingBinPriceSyncS2CPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = SellingBinMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SellingBinEvents {
    private SellingBinEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!serverLevel.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        if (SellingBinGroupManager.refreshForElapsedDays(serverLevel)) {
            syncAllPlayers(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        SellingBinGroupManager.invalidateCachedGroups();
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        var server = event.getPlayer() != null ? event.getPlayer().getServer() : ServerLifecycleHooks.getCurrentServer();
        ServerLevel overworld = server != null ? server.overworld() : null;
        if (overworld == null) {
            return;
        }

        SellingBinGroupManager.refreshForElapsedDays(overworld);
        if (event.getPlayer() != null) {
            syncPlayer(event.getPlayer());
        } else {
            syncAllPlayers(overworld);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SellingBinGroupManager.refreshForElapsedDays(serverPlayer.serverLevel());
            syncPlayer(serverPlayer);
        }
    }

    public static void syncAllPlayers(ServerLevel level) {
        ModMessages.get().send(PacketDistributor.ALL.noArg(), new SellingBinPriceSyncS2CPacket(
                SellingBinGroupManager.snapshotFloatingPriceBonuses(level),
                SellingBinGroupManager.snapshotVirtualStockPriceBonuses(level),
                SellingBinGroupManager.snapshotSeasonalPriceBonuses(level),
                SellingBinGroupManager.snapshotLongTermPriceBonuses(level)
        ));
    }

    public static void syncPlayer(ServerPlayer player) {
        ModMessages.get().send(PacketDistributor.PLAYER.with(() -> player), new SellingBinPriceSyncS2CPacket(
                SellingBinGroupManager.snapshotFloatingPriceBonuses(player.serverLevel()),
                SellingBinGroupManager.snapshotVirtualStockPriceBonuses(player.serverLevel()),
                SellingBinGroupManager.snapshotSeasonalPriceBonuses(player.serverLevel()),
                SellingBinGroupManager.snapshotLongTermPriceBonuses(player.serverLevel())
        ));
    }
}
