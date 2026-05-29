package com.y271727uy.sellingbin.all;

import com.y271727uy.sellingbin.SellingBinMod;
import com.y271727uy.sellingbin.client.menu.SellingBinMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, SellingBinMod.MODID);
    public static final RegistryObject<MenuType<SellingBinMenu>> SELLING_BIN = MENUS.register("selling_bin", () -> IForgeMenuType.create(ModMenus::createSellingBinMenu));

    private ModMenus() {
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }

    private static SellingBinMenu createSellingBinMenu(int windowId, Inventory inventory, FriendlyByteBuf data) {
        return new SellingBinMenu(SELLING_BIN.get(), windowId, inventory, data);
    }

    public static void open(ServerPlayer player, MenuProvider provider, BlockPos pos) {
        NetworkHooks.openScreen(player, provider, pos);
    }
}

