package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.neoforge.QuickCraftConfig;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import com.sxilverr.quickcraft.network.QuickCraftNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public class QuickCraftClientEvents {

    private static int pollTimer;
    private static int scrollTimer;

    private QuickCraftClientEvents() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(QuickCraftClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(QuickCraftClientEvents::onRenderGui);
        NeoForge.EVENT_BUS.addListener(QuickCraftClientEvents::onScreenKeyPressed);
        NeoForge.EVENT_BUS.addListener(QuickCraftClientEvents::onScreenMousePressed);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
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

    private static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || !BookmarkOverlay.isActive()) return;
        BookmarkOverlay.render(event.getGuiGraphics(),
                mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), false, 0, 0);
    }

    private static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() instanceof QuickCraftScreen) return;
        if (!QuickCraftClient.OPEN_KEY.matches(event.getKeyCode(), event.getScanCode())) return;
        if (tryOpenFromScreen(event.getScreen())) event.setCanceled(true);
    }

    private static void onScreenMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
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
