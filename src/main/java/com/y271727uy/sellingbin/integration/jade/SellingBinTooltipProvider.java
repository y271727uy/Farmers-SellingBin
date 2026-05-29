package com.y271727uy.sellingbin.integration.jade;

import com.y271727uy.sellingbin.SellingBinMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.lang.reflect.Method;

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
        if (blockEntity == null || !isSellingBin(blockEntity)) {
            return;
        }

        boolean bound = invokeBoolean(blockEntity, "isBound");
        if (bound) {
            boolean taxExempt = invokeBoolean(blockEntity, "isTaxExempt");
            Component accountType = Component.translatable(taxExempt
                    ? "tooltip.sellingbin.jade.account_premium"
                    : "tooltip.sellingbin.jade.account_normal").withStyle(taxExempt ? ChatFormatting.GOLD : ChatFormatting.GREEN);
            tooltip.add(Component.translatable("tooltip.sellingbin.jade.binding_account", accountType).withStyle(ChatFormatting.GRAY));
        } else {
            Component state = Component.translatable("tooltip.sellingbin.jade.unbound").withStyle(ChatFormatting.RED);
            tooltip.add(Component.translatable("tooltip.sellingbin.jade.status", state).withStyle(ChatFormatting.GRAY));
        }

        if (!bound) {
            return;
        }

        String boundPlayerName = invokeString(blockEntity, "getBoundPlayerName");
        Component playerComponent = (boundPlayerName == null || boundPlayerName.isBlank())
                ? Component.translatable("tooltip.sellingbin.jade.player_unknown")
                : Component.literal(boundPlayerName);
        tooltip.add(Component.translatable("tooltip.sellingbin.jade.player", playerComponent).withStyle(ChatFormatting.GRAY));
    }

    private static boolean isSellingBin(BlockEntity blockEntity) {
        return blockEntity.getClass().getName().equals("com.y271727uy.sellingbin.block.entity.SellingBinBlockEntity");
    }

    private static boolean invokeBoolean(BlockEntity blockEntity, String methodName) {
        Object value = invoke(blockEntity, methodName);
        return value instanceof Boolean booleanValue && booleanValue;
    }

    private static String invokeString(BlockEntity blockEntity, String methodName) {
        Object value = invoke(blockEntity, methodName);
        return value instanceof String stringValue ? stringValue : null;
    }

    private static Object invoke(BlockEntity blockEntity, String methodName) {
        try {
            Method method = blockEntity.getClass().getMethod(methodName);
            return method.invoke(blockEntity);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}

