package com.y271727uy.sellingbin;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = "sellingbin", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue ENABLE_EXAMPLE_FEATURE = BUILDER
            .comment("Whether the example config option is enabled.")
            .define("enableExampleFeature", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    public static boolean enableExampleFeature;

    private Config() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        enableExampleFeature = ENABLE_EXAMPLE_FEATURE.get();
    }
}
