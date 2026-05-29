package com.y271727uy.sellingbin.item;

import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@ParametersAreNonnullByDefault
public class GlowingItem extends Item {
	public GlowingItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean isFoil(@javax.annotation.Nullable ItemStack stack) {
		return false;
	}
}

