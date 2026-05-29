package com.y271727uy.sellingbin.client;

import com.mojang.datafixers.util.Either;
import com.y271727uy.sellingbin.SellingBinMod;
import com.y271727uy.sellingbin.all.ModBlockEntities;
import com.y271727uy.sellingbin.all.ModMenus;
import com.y271727uy.sellingbin.client.render.blockentity.SellingBinBlockEntityRenderer;
import com.y271727uy.sellingbin.client.render.model.SellingBinModel;
import com.y271727uy.sellingbin.client.screen.SellingBinScreen;
import com.y271727uy.sellingbin.client.sellingbin.SellingBinClientPriceCache;
import com.y271727uy.sellingbin.client.sellingbin.SellingBinClientPriceHelper;
import com.y271727uy.sellingbin.client.tooltip.SellingBinClientTooltipComponent;
import com.y271727uy.sellingbin.client.tooltip.SellingBinTooltipComponent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SellingBinMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ShopBinClientEvents {
    private ShopBinClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenus.SELLING_BIN.get(), SellingBinScreen::new));
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SellingBinModel.LAYER_LOCATION, SellingBinModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SELLING_BIN.get(), SellingBinBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(SellingBinTooltipComponent.class, SellingBinClientTooltipComponent::new);
    }

    @Mod.EventBusSubscriber(modid = SellingBinMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeBusEvents {
        private ForgeBusEvents() {
        }

        @SubscribeEvent
        public static void onRenderTooltipPre(RenderTooltipEvent.GatherComponents event) {
            SellingBinClientPriceHelper.findRecipe(event.getItemStack()).ifPresent(recipe -> event.getTooltipElements().add(Either.right(
                    new SellingBinTooltipComponent(
                            SellingBinClientPriceHelper.getDisplayInput(recipe, event.getItemStack()),
                            recipe.getInputCount(),
                            SellingBinClientPriceHelper.getDisplayOutput(recipe, event.getItemStack()),
                            SellingBinClientPriceHelper.getPriceText(recipe, event.getItemStack()),
                            SellingBinClientPriceHelper.getSeasonalBonusText(recipe, event.getItemStack())
                    )
            )));
        }

        @SubscribeEvent
        public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            SellingBinClientPriceCache.clear();
        }
    }
}


