package com.y271727uy.sellingbin.integration.sdm_integration.card;

import com.y271727uy.sellingbin.block.SellingBinBlock;
import com.y271727uy.sellingbin.block.entity.SellingBinBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@SuppressWarnings("unused")
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BankCardItem extends Item {
    private static final String TAG_BOUND_BIN_POS = "BoundSellingBinPos";
    private static final String TAG_BOUND_BIN_DIMENSION = "BoundSellingBinDimension";

    public BankCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        if (tryToggleStoredSellingBin(serverPlayer, stack)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return InteractionResultHolder.consume(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return useOnSellingBin(context, isTaxExempt());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.sellingbin.bank_card.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.sellingbin.bank_card.bind_hint").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.sellingbin.bank_card.unbind_hint").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.sellingbin.bank_card.notification_hint").withStyle(ChatFormatting.DARK_GRAY));
        appendTaxTooltip(tooltip);
    }

    protected boolean isTaxExempt() {
        return false;
    }

    protected void appendTaxTooltip(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.sellingbin.bank_card.tax_header").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.sellingbin.bank_card.tax_rule_1").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("tooltip.sellingbin.bank_card.tax_rule_2").withStyle(ChatFormatting.RED));
    }

    protected InteractionResult useOnSellingBin(UseOnContext context, boolean taxExempt) {
        Level level = context.getLevel();
        if (!(level.getBlockState(context.getClickedPos()).getBlock() instanceof SellingBinBlock)) {
            if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
                if (!level.isClientSide && context.getPlayer() instanceof ServerPlayer serverPlayer) {
                    tryToggleStoredSellingBin(serverPlayer, context.getItemInHand());
                }
                return level.isClientSide ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
            }

            return InteractionResult.PASS;
        }

        if (!(context.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof SellingBinBlockEntity sellingBin)) {
            return InteractionResult.CONSUME;
        }

        if (!sellingBin.isBound()) {
            if (!sellingBin.bindTo(serverPlayer, taxExempt)) {
                serverPlayer.displayClientMessage(Component.translatable("message.sellingbin.selling_bin.bind_fail").withStyle(ChatFormatting.RED), false);
                return InteractionResult.CONSUME;
            }

            rememberBoundSellingBin(context.getItemInHand(), level, context.getClickedPos());
            serverPlayer.displayClientMessage(Component.translatable("message.sellingbin.selling_bin.bind_success").withStyle(ChatFormatting.GREEN), false);
            return InteractionResult.CONSUME;
        }

        if (sellingBin.isBoundTo(serverPlayer)) {
            if (!sellingBin.unbind(serverPlayer)) {
                serverPlayer.displayClientMessage(Component.translatable("message.sellingbin.selling_bin.unbind_fail").withStyle(ChatFormatting.RED), false);
                return InteractionResult.CONSUME;
            }

            clearBoundSellingBin(context.getItemInHand());
            serverPlayer.displayClientMessage(Component.translatable("message.sellingbin.selling_bin.unbind_success").withStyle(ChatFormatting.GREEN), false);
            return InteractionResult.CONSUME;
        }

        if (sellingBin.isBound()) {
            serverPlayer.displayClientMessage(Component.translatable("message.sellingbin.selling_bin.bound_other").withStyle(ChatFormatting.RED), false);
            return InteractionResult.CONSUME;
        }

        return InteractionResult.CONSUME;
    }

    private boolean tryToggleStoredSellingBin(ServerPlayer serverPlayer, ItemStack stack) {
        SellingBinBlockEntity sellingBin = getStoredSellingBin(serverPlayer, stack);
        if (sellingBin == null || !sellingBin.isBoundTo(serverPlayer)) {
            sellingBin = findOwnedSellingBin(serverPlayer);
        }

        if (sellingBin == null || !sellingBin.isBoundTo(serverPlayer)) {
            return false;
        }

        boolean enabled = sellingBin.toggleTransactionNotification();
        serverPlayer.displayClientMessage(
                Component.translatable(enabled
                                ? "message.sellingbin.selling_bin.notification_enabled"
                                : "message.sellingbin.selling_bin.notification_disabled")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
                false
        );
        return true;
    }

    @Nullable
    private SellingBinBlockEntity findOwnedSellingBin(ServerPlayer serverPlayer) {
        SellingBinBlockEntity found = null;
        for (SellingBinBlockEntity candidate : SellingBinBlockEntity.getLoadedInstances()) {
            if (!candidate.isBoundTo(serverPlayer)) {
                continue;
            }

            if (found != null) {
                return null;
            }
            found = candidate;
        }

        return found;
    }

    @Nullable
    private SellingBinBlockEntity getStoredSellingBin(ServerPlayer serverPlayer, ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_BOUND_BIN_POS) || !tag.contains(TAG_BOUND_BIN_DIMENSION)) {
            return null;
        }

        ResourceLocation dimensionLocation = ResourceLocation.tryParse(tag.getString(TAG_BOUND_BIN_DIMENSION));
        if (dimensionLocation == null) {
            return null;
        }

        if (serverPlayer.getServer() == null) {
            return null;
        }

        ServerLevel targetLevel = serverPlayer.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimensionLocation));
        if (targetLevel == null) {
            return null;
        }

        BlockEntity blockEntity = targetLevel.getBlockEntity(BlockPos.of(tag.getLong(TAG_BOUND_BIN_POS)));
        return blockEntity instanceof SellingBinBlockEntity sellingBin ? sellingBin : null;
    }

    private void rememberBoundSellingBin(ItemStack stack, Level level, BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(TAG_BOUND_BIN_POS, pos.asLong());
        tag.putString(TAG_BOUND_BIN_DIMENSION, level.dimension().location().toString());
    }

    private void clearBoundSellingBin(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return;
        }

        tag.remove(TAG_BOUND_BIN_POS);
        tag.remove(TAG_BOUND_BIN_DIMENSION);
        if (tag.isEmpty()) {
            stack.setTag(null);
        }
    }
}
