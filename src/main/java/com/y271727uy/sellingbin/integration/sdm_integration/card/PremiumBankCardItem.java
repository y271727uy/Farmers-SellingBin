package com.y271727uy.sellingbin.integration.sdm_integration.card;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class PremiumBankCardItem extends BankCardItem {
    public PremiumBankCardItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected boolean isTaxExempt() {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.sellingbin.premium_bank_card.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.sellingbin.premium_bank_card.bind_hint").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.sellingbin.premium_bank_card.unbind_hint").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.sellingbin.bank_card.notification_hint").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.sellingbin.premium_bank_card.tax_free").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(UseOnContext context) {
        return useOnSellingBin(context, isTaxExempt());
    }
}
