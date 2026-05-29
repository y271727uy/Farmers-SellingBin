package com.y271727uy.sellingbin.network;

import com.y271727uy.sellingbin.client.sellingbin.SellingBinClientPriceCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class SellingBinPriceSyncS2CPacket {
    private final Map<ResourceLocation, Integer> floatingPriceBonusByRecipe;
    private final Map<ResourceLocation, Integer> virtualStockPriceBonusByRecipe;
    private final Map<ResourceLocation, Integer> seasonalPriceBonusByRecipe;
    private final Map<ResourceLocation, Integer> longTermPriceBonusByRecipe;

    public SellingBinPriceSyncS2CPacket(
            Map<ResourceLocation, Integer> floatingPriceBonusByRecipe,
            Map<ResourceLocation, Integer> virtualStockPriceBonusByRecipe,
            Map<ResourceLocation, Integer> seasonalPriceBonusByRecipe,
            Map<ResourceLocation, Integer> longTermPriceBonusByRecipe
    ) {
        this.floatingPriceBonusByRecipe = Map.copyOf(floatingPriceBonusByRecipe);
        this.virtualStockPriceBonusByRecipe = Map.copyOf(virtualStockPriceBonusByRecipe);
        this.seasonalPriceBonusByRecipe = Map.copyOf(seasonalPriceBonusByRecipe);
        this.longTermPriceBonusByRecipe = Map.copyOf(longTermPriceBonusByRecipe);
    }

    public static void encode(SellingBinPriceSyncS2CPacket packet, FriendlyByteBuf buf) {
        writeBonusMap(buf, packet.floatingPriceBonusByRecipe);
        writeBonusMap(buf, packet.virtualStockPriceBonusByRecipe);
        writeBonusMap(buf, packet.seasonalPriceBonusByRecipe);
        writeBonusMap(buf, packet.longTermPriceBonusByRecipe);
    }

    public static SellingBinPriceSyncS2CPacket decode(FriendlyByteBuf buf) {
        Map<ResourceLocation, Integer> floatingPriceBonusByRecipe = readBonusMap(buf);
        Map<ResourceLocation, Integer> virtualStockPriceBonusByRecipe = readBonusMap(buf);
        Map<ResourceLocation, Integer> seasonalPriceBonusByRecipe = readBonusMap(buf);
        Map<ResourceLocation, Integer> longTermPriceBonusByRecipe = readBonusMap(buf);
        return new SellingBinPriceSyncS2CPacket(floatingPriceBonusByRecipe, virtualStockPriceBonusByRecipe, seasonalPriceBonusByRecipe, longTermPriceBonusByRecipe);
    }

    public static void handle(SellingBinPriceSyncS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                SellingBinClientPriceCache.applyExtendedSnapshot(packet.floatingPriceBonusByRecipe, packet.virtualStockPriceBonusByRecipe, packet.seasonalPriceBonusByRecipe, packet.longTermPriceBonusByRecipe)
        ));
        context.setPacketHandled(true);
    }

    private static void writeBonusMap(FriendlyByteBuf buf, Map<ResourceLocation, Integer> bonusByRecipe) {
        buf.writeVarInt(bonusByRecipe.size());
        bonusByRecipe.forEach((recipeId, bonus) -> {
            buf.writeResourceLocation(recipeId);
            buf.writeVarInt(bonus);
        });
    }

    private static Map<ResourceLocation, Integer> readBonusMap(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<ResourceLocation, Integer> bonusByRecipe = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            bonusByRecipe.put(buf.readResourceLocation(), buf.readVarInt());
        }
        return bonusByRecipe;
    }
}

