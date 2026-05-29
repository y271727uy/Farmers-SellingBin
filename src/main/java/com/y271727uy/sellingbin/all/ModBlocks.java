package com.y271727uy.sellingbin.all;

import com.y271727uy.sellingbin.SellingBinMod;
import com.y271727uy.sellingbin.block.SellingBinBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SellingBinMod.MODID);

	public static final RegistryObject<Block> SELLING_BIN = BLOCKS.register("selling_bin", () -> new SellingBinBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).noOcclusion()));

	private ModBlocks() {
	}

	public static void register(IEventBus eventBus) {
		BLOCKS.register(eventBus);
	}
}
