package com.y271727uy.sellingbin.all;

import com.y271727uy.sellingbin.SellingBinMod;
import com.y271727uy.sellingbin.integration.sdm_integration.card.BankCardItem;
import com.y271727uy.sellingbin.integration.sdm_integration.card.PremiumBankCardItem;
import com.y271727uy.sellingbin.item.GlowingItem;
import com.y271727uy.sellingbin.item.SellingBinBlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SellingBinMod.MODID);

	public static final RegistryObject<Item> SELLING_BIN = ITEMS.register("selling_bin", () -> new SellingBinBlockItem(ModBlocks.SELLING_BIN.get(), new Item.Properties()));
	public static final RegistryObject<Item> BANK_CARD = ITEMS.register("bank_card", () -> new BankCardItem(new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> PREMIUM_BANK_CARD = ITEMS.register("premium_bank_card", () -> new PremiumBankCardItem(new Item.Properties().stacksTo(1)));
	public static final RegistryObject<Item> EQUALS = ITEMS.register("equals", () -> new GlowingItem(new Item.Properties()));

	public static final RegistryObject<Item> COPPER_GT_CREDIT = ITEMS.register("copper_gt_credit", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> CUPRONICKEL_GT_CREDIT = ITEMS.register("cupronickel_gt_credit", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> SILVER_GT_CREDIT = ITEMS.register("silver_gt_credit", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> GOLD_GT_CREDIT = ITEMS.register("gold_gt_credit", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> PLATINUM_GT_CREDIT = ITEMS.register("platinum_gt_credit", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> OSMIUM_GT_CREDIT = ITEMS.register("osmium_gt_credit", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> NAQUADAH_GT_CREDIT = ITEMS.register("naquadah_gt_credit", () -> new Item(new Item.Properties()));
	public static final RegistryObject<Item> DOGE_COIN = ITEMS.register("doge_coin", () -> new Item(new Item.Properties()));

	public static final RegistryObject<Item> IRON_STAR = ITEMS.register("iron_star", () -> new GlowingItem(new Item.Properties()));
	public static final RegistryObject<Item> GOLD_STAR = ITEMS.register("gold_star", () -> new GlowingItem(new Item.Properties()));
	public static final RegistryObject<Item> DIAMOND_STAR = ITEMS.register("diamond_star", () -> new GlowingItem(new Item.Properties()));

	private ModItems() {
	}

	public static void register(IEventBus eventBus) {
		ITEMS.register(eventBus);
	}
}
