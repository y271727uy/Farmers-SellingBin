package com.y271727uy.sellingbin.integration.jade.provider;

import com.y271727uy.sellingbin.SellingBinMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

@SuppressWarnings("unused")
public enum SellingBinTooltipProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(SellingBinMod.MODID, "selling_bin_jade");

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (!(blockEntity instanceof com.y271727uy.sellingbin.block.entity.SellingBinBlockEntity)) {
            return;
        }

        com.y271727uy.sellingbin.block.entity.SellingBinBlockEntity sellingBin = (com.y271727uy.sellingbin.block.entity.SellingBinBlockEntity) blockEntity;

        boolean bound = sellingBin.isBound();
        if (bound) {
            Component accountType = Component.translatable(sellingBin.isTaxExempt()
                    ? "tooltip.sellingbin.jade.account_premium"
                    : "tooltip.sellingbin.jade.account_normal").withStyle(sellingBin.isTaxExempt() ? ChatFormatting.GOLD : ChatFormatting.GREEN);
            tooltip.add(Component.translatable("tooltip.sellingbin.jade.binding_account", accountType).withStyle(ChatFormatting.GRAY));
        } else {
            Component state = Component.translatable("tooltip.sellingbin.jade.unbound").withStyle(ChatFormatting.RED);
            tooltip.add(Component.translatable("tooltip.sellingbin.jade.status", state).withStyle(ChatFormatting.GRAY));
        }

        if (!bound) {
            return;
        }

        String boundPlayerName = sellingBin.getBoundPlayerName();
        Component playerComponent = (boundPlayerName == null || boundPlayerName.isBlank())
                ? Component.translatable("tooltip.sellingbin.jade.player_unknown")
                : Component.literal(boundPlayerName);
        tooltip.add(Component.translatable("tooltip.sellingbin.jade.player", playerComponent).withStyle(ChatFormatting.GRAY));
    }
}

