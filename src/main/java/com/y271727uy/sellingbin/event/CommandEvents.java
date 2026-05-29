package com.y271727uy.sellingbin.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.y271727uy.sellingbin.SellingBinMod;
import com.y271727uy.sellingbin.block.entity.SellingBinBlockEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SellingBinMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommandEvents {
    private CommandEvents() { }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getServer().getCommands().getDispatcher();
        dispatcher.register(Commands.literal("sellingbin").then(Commands.literal("sell").requires(source -> source.hasPermission(2)).executes(CommandEvents::executeSellCommand)));
    }

    private static int executeSellCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        int processedCount = 0;
        for (SellingBinBlockEntity sellingBin : SellingBinBlockEntity.getLoadedInstances()) {
            if (sellingBin.getLevel() == level) {
                SellingBinBlockEntity.runAllRecipesBroadcast(level, sellingBin);
                processedCount++;
            }
        }
        final int resultCount = processedCount;
        source.sendSuccess(() -> resultCount > 0
                ? net.minecraft.network.chat.Component.translatable("message.sellingbin.command.sell.success", resultCount)
                : net.minecraft.network.chat.Component.translatable("message.sellingbin.command.sell.none"), true);
        return processedCount;
    }
}


