package com.y271727uy.sellingbin.client.screen;

import com.y271727uy.sellingbin.client.menu.SellingBinMenu;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SellingBinScreen extends AbstractContainerScreen<SellingBinMenu> {
    public static final ResourceLocation BACKGROUND = new ResourceLocation("sellingbin", "textures/gui/screen/selling_bin.png");

    public SellingBinScreen(@Nonnull SellingBinMenu menu, @Nonnull Inventory playerInventory, @Nonnull Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 174;
        this.imageHeight = 164;
    }

    @Override
    public void render(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        MutableComponent timeText = formatTicks(menu.getTicksUntilRun());
        guiGraphics.drawString(this.font, timeText, this.leftPos + 150, this.topPos + 7, 0xFFFFFF, false);

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static MutableComponent formatTicks(int ticks) {
        int totalSeconds = Math.max(0, ticks) / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return Component.literal(String.format("%d:%02d", minutes, seconds));
    }

    @Override
    protected void renderBg(@Nonnull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int i = (this.width - this.imageWidth) / 2;
        int j = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(BACKGROUND, i, j, 0, 0, this.imageWidth, this.imageHeight, 512, 256);
    }
}



