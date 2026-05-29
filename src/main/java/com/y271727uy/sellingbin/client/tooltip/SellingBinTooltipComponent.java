package com.y271727uy.sellingbin.client.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public record SellingBinTooltipComponent(ItemStack inputPreview, int inputCount, ItemStack output, String outputPriceText, String seasonalBonusText) implements TooltipComponent {
}


