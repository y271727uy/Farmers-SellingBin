package com.y271727uy.sellingbin.all;

import com.y271727uy.sellingbin.SellingBinMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@SuppressWarnings("unused")
public final class ModCreativeModeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SellingBinMod.MODID);

    @SuppressWarnings("unused")
    public static final RegistryObject<CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("block.sellingbin.selling_bin"))
            .icon(() -> new ItemStack(ModItems.SELLING_BIN.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModItems.SELLING_BIN.get());
                output.accept(ModItems.BANK_CARD.get());
                output.accept(ModItems.PREMIUM_BANK_CARD.get());
                output.accept(ModItems.COPPER_GT_CREDIT.get());
                output.accept(ModItems.CUPRONICKEL_GT_CREDIT.get());
                output.accept(ModItems.SILVER_GT_CREDIT.get());
                output.accept(ModItems.GOLD_GT_CREDIT.get());
                output.accept(ModItems.PLATINUM_GT_CREDIT.get());
                output.accept(ModItems.OSMIUM_GT_CREDIT.get());
                output.accept(ModItems.NAQUADAH_GT_CREDIT.get());
                output.accept(ModItems.DOGE_COIN.get());
            })
            .build());

    private ModCreativeModeTab() {
    }

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}



