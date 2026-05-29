package com.y271727uy.sellingbin.client.tooltip;

import com.y271727uy.sellingbin.all.ModItems;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@SuppressWarnings("unused")
public class SellingBinClientTooltipComponent implements ClientTooltipComponent {
    private static final int SLOT_SIZE = 18;
    private static final int SEPARATOR_W = 18;
    private static final int OVERLAY_PADDING = 1;
    private static final int OVERLAY_Z_OFFSET = 200;
    private static final int OVERLAY_OFFSET_X = 6;
    private static final int OVERLAY_OFFSET_Y = 2;
    private static final int OVERLAY_SHADOW_EXTENT = 1;
    private static final int OVERLAY_EXTRA_WIDTH = Math.max(0, OVERLAY_OFFSET_X) + OVERLAY_SHADOW_EXTENT;
    private static final int OVERLAY_EXTRA_HEIGHT = Math.max(0, OVERLAY_OFFSET_Y) + OVERLAY_SHADOW_EXTENT;
    private static final int SEASON_LINE_HEIGHT = 10;
    private static final int SEASON_LINE_COLOR = 0xFFFFFF;

    private final ItemStack inputPreview;
    private final int inputCount;
    private final ItemStack output;
    private final String outputPriceText;
    private final String seasonalBonusText;

    public SellingBinClientTooltipComponent(SellingBinTooltipComponent component) {
        this.output = component.output().copy();
        this.outputPriceText = component.outputPriceText();
        this.seasonalBonusText = component.seasonalBonusText();
        this.inputPreview = component.inputPreview().copy();
        this.inputCount = Math.max(1, component.inputCount());
    }

    @Override
    public int getHeight() {
        return SLOT_SIZE + OVERLAY_EXTRA_HEIGHT + (seasonalBonusText.isEmpty() ? 0 : SEASON_LINE_HEIGHT);
    }

    @Override
    public int getWidth(Font font) {
        int baseWidth = SLOT_SIZE + SEPARATOR_W + SLOT_SIZE + OVERLAY_EXTRA_WIDTH;
        if (seasonalBonusText.isEmpty()) {
            return baseWidth;
        }

        return Math.max(baseWidth, font.width(seasonalBonusText) + 2);
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        if (!inputPreview.isEmpty()) {
            guiGraphics.renderItem(inputPreview, x + 1, y + 1);
            renderStackCount(font, guiGraphics, inputCount, x + 1, y + 1);
        }

        int separatorX = x + SLOT_SIZE;
        guiGraphics.renderItem(ModItems.EQUALS.get().getDefaultInstance(), separatorX + 1, y + 1);

        if (!output.isEmpty()) {
            int outX = x + SLOT_SIZE + SEPARATOR_W;
            guiGraphics.renderItem(output, outX + 1, y + 1);
            renderOverlayText(font, guiGraphics, outX, y, outputPriceText);
        }

        if (!seasonalBonusText.isEmpty()) {
            guiGraphics.drawString(font, seasonalBonusText, x + 1, y + SLOT_SIZE + 2, SEASON_LINE_COLOR, true);
        }
    }

    private static void renderStackCount(Font font, GuiGraphics guiGraphics, int count, int slotX, int slotY) {
        if (count <= 1) {
            return;
        }

        String text = Integer.toString(count);
        int textWidth = Math.max(1, font.width(text));
        float scale = Math.min(1.0F, 16.0F / textWidth);
        float inverseScale = 1.0F / scale;

        int scaledTextWidth = Math.round(textWidth * scale);
        int drawX = slotX + SLOT_SIZE - OVERLAY_PADDING - scaledTextWidth;
        int drawY = slotY + SLOT_SIZE - OVERLAY_PADDING - Math.round(font.lineHeight * scale);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, OVERLAY_Z_OFFSET);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, text, Math.round(drawX * inverseScale), Math.round(drawY * inverseScale), 0xFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    private static void renderOverlayText(Font font, GuiGraphics guiGraphics, int slotX, int slotY, String text) {
        if (text.isEmpty()) {
            return;
        }

        int textWidth = Math.max(1, font.width(text));
        float scale = Math.min(1.0F, 16.0F / textWidth);
        float inverseScale = 1.0F / scale;

        int scaledTextWidth = Math.round(textWidth * scale);
        int drawX = slotX + SLOT_SIZE - OVERLAY_PADDING - scaledTextWidth + OVERLAY_OFFSET_X;
        int drawY = slotY + SLOT_SIZE - OVERLAY_PADDING - Math.round(font.lineHeight * scale) + OVERLAY_OFFSET_Y;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, OVERLAY_Z_OFFSET);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, text, Math.round(drawX * inverseScale), Math.round(drawY * inverseScale), 0xFFFFFF, true);
        guiGraphics.pose().popPose();
    }
}


