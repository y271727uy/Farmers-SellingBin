package com.y271727uy.sellingbin.all;

import com.y271727uy.sellingbin.SellingBinMod;
import com.y271727uy.sellingbin.block.entity.SellingBinBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SellingBinMod.MODID);
    public static final RegistryObject<BlockEntityType<SellingBinBlockEntity>> SELLING_BIN = BLOCK_ENTITY_TYPES.register("selling_bin", () -> BlockEntityType.Builder.of(SellingBinBlockEntity::new, ModBlocks.SELLING_BIN.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}

