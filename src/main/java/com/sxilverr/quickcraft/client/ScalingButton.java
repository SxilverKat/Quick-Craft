package com.sxilverr.quickcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

public class ScalingButton extends GuiButton {
    private static final int BAND_HEIGHT = 20;
    private static final int EDGE = BAND_HEIGHT / 2;

    public ScalingButton(int id, int x, int y, int width, int height, String text) {
        super(id, x, y, width, height, text);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) return;

        FontRenderer font = mc.fontRenderer;
        mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.hovered = mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height;
        int v = 46 + this.getHoverState(this.hovered) * BAND_HEIGHT;

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        int top = Math.min(this.height / 2, EDGE);
        int bottom = this.height - top;
        int half = this.width / 2;
        int rightU = 200 - half;

        drawTexturedModalRect(this.x, this.y, 0, v, half, top);
        drawTexturedModalRect(this.x, this.y + top, 0, v + BAND_HEIGHT - bottom, half, bottom);
        drawTexturedModalRect(this.x + half, this.y, rightU, v, half, top);
        drawTexturedModalRect(this.x + half, this.y + top, rightU, v + BAND_HEIGHT - bottom, half, bottom);

        this.mouseDragged(mc, mouseX, mouseY);

        int color = 14737632;
        if (this.packedFGColour != 0) {
            color = this.packedFGColour;
        } else if (!this.enabled) {
            color = 10526880;
        } else if (this.hovered) {
            color = 16777120;
        }
        this.drawCenteredString(font, this.displayString,
                this.x + this.width / 2, this.y + (this.height - 8) / 2, color);
    }
}
