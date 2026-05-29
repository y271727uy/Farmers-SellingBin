package com.y271727uy.sellingbin.all;

import com.y271727uy.sellingbin.SellingBinMod;
import com.y271727uy.sellingbin.recipe.SellingBinRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SellingBinMod.MODID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, SellingBinMod.MODID);

    public static final RegistryObject<RecipeSerializer<SellingBinRecipe>> SELLING_BIN_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("selling_bin", SellingBinRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<SellingBinRecipe>> SELLING_BIN_RECIPE_TYPE = RECIPE_TYPES.register("selling_bin", () -> new RecipeType<>() {
        @Override
        public String toString() {
            return SellingBinMod.MODID + ":selling_bin";
        }
    });

    private ModRecipes() {
    }

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
        RECIPE_TYPES.register(eventBus);
    }
}

