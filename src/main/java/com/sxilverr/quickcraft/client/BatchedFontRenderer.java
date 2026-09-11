package com.sxilverr.quickcraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

public final class BatchedFontRenderer extends FontRenderer {
    private static final ResourceLocation ASCII = new ResourceLocation("textures/font/ascii.png");
    private static BatchedFontRenderer instance;

    private final TextureManager textures;
    private final BufferBuilder buffer = new BufferBuilder(4096);
    private final WorldVertexBufferUploader uploader = new WorldVertexBufferUploader();
    private boolean batching;
    private boolean pending;
    private float r = 1.0F;
    private float g = 1.0F;
    private float b = 1.0F;
    private float a = 1.0F;

    private BatchedFontRenderer(Minecraft mc) {
        super(mc.gameSettings, ASCII, mc.getTextureManager(), false);
        this.textures = mc.getTextureManager();
    }

    public static FontRenderer get(Minecraft mc) {
        if (mc == null || mc.fontRenderer == null) return mc == null ? null : mc.fontRenderer;
        if (instance == null) {
            instance = new BatchedFontRenderer(mc);
            IResourceManager manager = mc.getResourceManager();
            instance.onResourceManagerReload(manager);
            if (manager instanceof IReloadableResourceManager) {
                ((IReloadableResourceManager) manager).registerReloadListener(instance);
            }
        }
        instance.setUnicodeFlag(mc.fontRenderer.getUnicodeFlag());
        instance.setBidiFlag(mc.fontRenderer.getBidiFlag());
        return instance;
    }

    @Override
    public int drawString(String text, float x, float y, int color, boolean dropShadow) {
        if (batching) return super.drawString(text, x, y, color, dropShadow);
        batching = true;
        try {
            int width = super.drawString(text, x, y, color, dropShadow);
            flush();
            return width;
        } finally {
            batching = false;
        }
    }

    @Override
    protected void setColor(float red, float green, float blue, float alpha) {
        super.setColor(red, green, blue, alpha);
        this.r = red;
        this.g = green;
        this.b = blue;
        this.a = alpha;
    }

    @Override
    protected float renderDefaultChar(int ch, boolean italic) {
        if (!batching) return super.renderDefaultChar(ch, italic);
        int u = ch % 16 * 8;
        int v = ch / 16 * 8;
        float skew = italic ? 1.0F : 0.0F;
        int width = this.charWidth[ch];
        float w = width - 0.01F;
        float x0 = this.posX;
        float x1 = this.posX + w - 1.0F;
        float y0 = this.posY;
        float y1 = this.posY + 7.99F;
        float u0 = u / 128.0F;
        float u1 = (u + w - 1.0F) / 128.0F;
        float v0 = v / 128.0F;
        float v1 = (v + 7.99F) / 128.0F;
        begin();
        vertex(x0 + skew, y0, u0, v0);
        vertex(x0 - skew, y1, u0, v1);
        vertex(x1 - skew, y1, u1, v1);
        vertex(x1 + skew, y0, u1, v0);
        return width;
    }

    @Override
    protected float renderUnicodeChar(char ch, boolean italic) {
        flush();
        return super.renderUnicodeChar(ch, italic);
    }

    private void begin() {
        if (pending) return;
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
        pending = true;
    }

    private void vertex(float x, float y, float u, float v) {
        buffer.pos(x, y, 0.0D).tex(u, v).color(r, g, b, a).endVertex();
    }

    private void flush() {
        if (!pending) return;
        pending = false;
        textures.bindTexture(this.locationFontTexture);
        buffer.finishDrawing();
        uploader.draw(buffer);
    }
}
