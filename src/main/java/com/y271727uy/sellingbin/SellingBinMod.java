package com.y271727uy.sellingbin;

import com.mojang.logging.LogUtils;
import com.y271727uy.sellingbin.all.ModBlockEntities;
import com.y271727uy.sellingbin.all.ModBlocks;
import com.y271727uy.sellingbin.all.ModCreativeModeTab;
import com.y271727uy.sellingbin.all.ModItems;
import com.y271727uy.sellingbin.all.ModMenus;
import com.y271727uy.sellingbin.all.ModRecipes;
import com.y271727uy.sellingbin.network.ModMessages;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(SellingBinMod.MODID)
public class SellingBinMod {
    public static final String MODID = "sellingbin";
    private static final Logger LOGGER = LogUtils.getLogger();

    public SellingBinMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeModeTab.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModRecipes.register(modEventBus);
        ModMessages.register();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("{} loaded successfully.", MODID);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Client setup for {}", MODID);
    }
}
