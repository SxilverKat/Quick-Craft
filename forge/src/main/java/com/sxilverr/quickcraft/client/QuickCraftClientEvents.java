package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.forge.QuickCraftConfig;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import com.sxilverr.quickcraft.network.QuickCraftNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = QuickCraftCommon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class QuickCraftClientEvents {

    private static int pollTimer;
    private static int scrollTimer;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (BookmarkOverlay.isActive() && ++pollTimer >= 20) {
            pollTimer = 0;
            QuickCraftNetwork.requestAvailability(BookmarkOverlay.requestedKeys());
        }

        if (mc.screen != null) return;

        while (QuickCraftClient.OPEN_KEY.consumeClick()) {
            ItemStack target = mc.player.getMainHandItem();
            if (target.isEmpty()) continue;
            if (QuickCraftConfig.openOnlyWithRecipe() && mc.level != null
                    && !ClientRecipeCache.hasRecipe(mc.level, target)) continue;
            mc.setScreen(new QuickCraftScreen(target.copy(), 1));
        }

        if (BookmarkOverlay.isActive()) {
            boolean up = QuickCraftClient.SCROLL_UP_KEY.isDown();
            boolean down = QuickCraftClient.SCROLL_DOWN_KEY.isDown();
            if (up ^ down) {
                if (scrollTimer <= 0) {
                    BookmarkOverlay.scroll(up ? -1 : 1);
                    scrollTimer = 3;
                } else {
                    scrollTimer--;
                }
            } else {
                scrollTimer = 0;
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || !BookmarkOverlay.isActive()) return;
        BookmarkOverlay.render(event.getGuiGraphics(),
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), false, 0, 0);
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() instanceof QuickCraftScreen) return;
        if (!QuickCraftClient.OPEN_KEY.matches(event.getKeyCode(), event.getScanCode())) return;
        if (TextInputGuard.isTyping(event.getScreen())) return;
        if (tryOpenFromScreen(event.getScreen())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getScreen() instanceof QuickCraftScreen) return;
        if (!QuickCraftClient.OPEN_KEY.matchesMouse(event.getButton())) return;
        if (tryOpenFromScreen(event.getScreen())) event.setCanceled(true);
    }

    private static boolean tryOpenFromScreen(Screen screen) {
        ItemStack target = hoveredTarget(screen);
        if (target.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (QuickCraftConfig.openOnlyWithRecipe() && mc.level != null
                && !ClientRecipeCache.hasRecipe(mc.level, target)) {
            return true;
        }
        if (screen instanceof AbstractContainerScreen<?> && mc.player != null) {
            mc.player.closeContainer();
        }
        mc.setScreen(new QuickCraftScreen(target.copy(), 1));
        return true;
    }

    private static ItemStack hoveredTarget(Screen screen) {
        ItemStack jei = QuickCraftIntegrations.hoveredItem();
        if (!jei.isEmpty()) return jei;
        if (screen instanceof AbstractContainerScreen<?> container) {
            Slot slot = container.getSlotUnderMouse();
            if (slot != null && slot.hasItem()) return slot.getItem();
        }
        return ItemStack.EMPTY;
    }
}
