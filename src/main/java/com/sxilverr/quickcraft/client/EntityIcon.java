package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.QuickCraftConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public final class EntityIcon {
    private static final long SPIN_PERIOD_MS = 15000L;
    private static final long ANIM_TICK_INTERVAL_MS = 50L;
    private static final float WALK_ANIM_SPEED = 0.6F;
    private static final float TILT_DEGREES = 15.0F;

    private static final Map<ResourceLocation, Holder> CACHE = new HashMap<ResourceLocation, Holder>();
    private static World cacheWorld;

    private EntityIcon() {
    }

    private static final class Holder {
        Entity entity;
        long lastAnimTickMs;
    }

    private static Holder holder(ResourceLocation entityId) {
        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        if (world == null) return null;
        if (cacheWorld != world) {
            cacheWorld = world;
            CACHE.clear();
        }
        Holder holder = CACHE.get(entityId);
        if (holder == null) {
            holder = new Holder();
            try {
                holder.entity = EntityList.createEntityByIDFromName(entityId, world);
            } catch (Throwable ignored) {
            }
            CACHE.put(entityId, holder);
        }
        return holder;
    }

    public static void render(int x, int y, int w, int h, ResourceLocation entityId) {
        if (entityId == null) {
            fallback(x, y, w, h);
            return;
        }
        Holder holder = holder(entityId);
        if (holder == null || !(holder.entity instanceof EntityLivingBase)) {
            fallback(x, y, w, h);
            return;
        }
        EntityLivingBase entity = (EntityLivingBase) holder.entity;
        advance(entity, holder);

        float bbHeight = Math.max(entity.height, 0.1F);
        float bbWidth = Math.max(entity.width, 0.1F);
        float scale = Math.min(h / bbHeight, w / bbWidth);
        if (scale <= 0.0F) {
            fallback(x, y, w, h);
            return;
        }

        int cx = x + w / 2;
        int cy = (int) (y + h / 2.0F + bbHeight * scale / 2.0F);

        float yaw = QuickCraftConfig.entitySpin()
                ? (System.currentTimeMillis() % SPIN_PERIOD_MS) / (float) SPIN_PERIOD_MS * 360.0F
                * (float) QuickCraftConfig.entitySpinSpeed()
                : 0.0F;

        float prevRenderYawOffset = entity.renderYawOffset;
        float prevRotationYaw = entity.rotationYaw;
        float prevRotationPitch = entity.rotationPitch;
        float prevRotationYawHead = entity.rotationYawHead;
        float prevPrevRotationYawHead = entity.prevRotationYawHead;

        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, cy, 50.0F);
        GlStateManager.scale(-scale, scale, scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-TILT_DEGREES, 1.0F, 0.0F, 0.0F);

        entity.renderYawOffset = yaw;
        entity.rotationYaw = yaw;
        entity.rotationPitch = 0.0F;
        entity.rotationYawHead = yaw;
        entity.prevRotationYawHead = yaw;

        RenderManager manager = Minecraft.getMinecraft().getRenderManager();
        manager.setPlayerViewY(180.0F);
        manager.setRenderShadow(false);
        try {
            manager.renderEntity(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
        } catch (Throwable ignored) {
        } finally {
            manager.setRenderShadow(true);

            entity.renderYawOffset = prevRenderYawOffset;
            entity.rotationYaw = prevRotationYaw;
            entity.rotationPitch = prevRotationPitch;
            entity.rotationYawHead = prevRotationYawHead;
            entity.prevRotationYawHead = prevPrevRotationYawHead;

            GlStateManager.popMatrix();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static void advance(EntityLivingBase entity, Holder holder) {
        boolean idle = QuickCraftConfig.entityIdle();
        boolean walk = QuickCraftConfig.entityWalk();
        if (!idle && !walk) return;
        long now = System.currentTimeMillis();
        if (holder.lastAnimTickMs == 0L) {
            holder.lastAnimTickMs = now;
            return;
        }
        long elapsed = now - holder.lastAnimTickMs;
        if (elapsed < ANIM_TICK_INTERVAL_MS) return;
        long ticks;
        if (elapsed > ANIM_TICK_INTERVAL_MS * 4L) {
            ticks = 1L;
            holder.lastAnimTickMs = now;
        } else {
            ticks = elapsed / ANIM_TICK_INTERVAL_MS;
            holder.lastAnimTickMs += ticks * ANIM_TICK_INTERVAL_MS;
        }
        for (long i = 0; i < ticks; i++) {
            if (idle) entity.ticksExisted++;
            if (walk) {
                entity.prevLimbSwingAmount = entity.limbSwingAmount;
                entity.limbSwingAmount = WALK_ANIM_SPEED;
                entity.limbSwing += WALK_ANIM_SPEED;
            }
        }
    }

    private static void fallback(int x, int y, int w, int h) {
        Draw.item(new ItemStack(Blocks.MOB_SPAWNER), x + (w - 16) / 2, y + (h - 16) / 2);
    }
}
