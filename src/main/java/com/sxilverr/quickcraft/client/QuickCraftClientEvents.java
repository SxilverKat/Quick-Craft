package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.QuickCraft;
import com.sxilverr.quickcraft.QuickCraftConfig;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import com.sxilverr.quickcraft.network.QuickCraftNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

@Mod.EventBusSubscriber(modid = QuickCraft.MODID, value = Side.CLIENT)
@SideOnly(Side.CLIENT)
public final class QuickCraftClientEvents {
    private static int pollTimer;
    private static int scrollTimer;

    private QuickCraftClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) return;

        if (BookmarkOverlay.isActive() && ++pollTimer >= 20) {
            pollTimer = 0;
            QuickCraftNetwork.requestAvailability(BookmarkOverlay.requestedKeys());
        }

        if (mc.currentScreen != null) return;

        while (QuickCraftKeys.OPEN.isPressed()) {
            ItemStack target = mc.player.getHeldItemMainhand();
            if (target.isEmpty()) continue;
            if (QuickCraftConfig.openOnlyWithRecipe() && !ClientRecipeCache.hasRecipe(target)) continue;
            mc.displayGuiScreen(new QuickCraftScreen(target.copy(), 1));
        }

        if (BookmarkOverlay.isActive()) {
            boolean up = QuickCraftKeys.SCROLL_UP.isKeyDown();
            boolean down = QuickCraftKeys.SCROLL_DOWN.isKeyDown();
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
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null || mc.player == null || !BookmarkOverlay.isActive()) return;
        ScaledResolution sr = new ScaledResolution(mc);
        BookmarkOverlay.render(sr.getScaledWidth(), sr.getScaledHeight(), false, 0, 0);
    }

    @SubscribeEvent
    public static void onScreenKeyboard(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (event.getGui() instanceof QuickCraftScreen) return;
        if (!Keyboard.getEventKeyState()) return;
        if (!QuickCraftKeys.matches(QuickCraftKeys.OPEN, Keyboard.getEventKey())) return;
        if (TextInputGuard.isTyping(event.getGui())) return;
        if (tryOpenFromScreen(event.getGui())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenMouse(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (event.getGui() instanceof QuickCraftScreen) return;
        int button = Mouse.getEventButton();
        if (button < 0 || !Mouse.getEventButtonState()) return;
        if (QuickCraftKeys.OPEN.getKeyCode() != button - 100) return;
        if (tryOpenFromScreen(event.getGui())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ClientRecipeCache.clear();
        BookmarkOverlay.clear();
    }

    private static boolean tryOpenFromScreen(GuiScreen screen) {
        ItemStack target = hoveredTarget(screen);
        if (target.isEmpty()) return false;
        Minecraft mc = Minecraft.getMinecraft();
        if (QuickCraftConfig.openOnlyWithRecipe() && !ClientRecipeCache.hasRecipe(target)) {
            return true;
        }
        if (screen instanceof GuiContainer && mc.player != null) {
            mc.player.closeScreen();
        }
        mc.displayGuiScreen(new QuickCraftScreen(target.copy(), 1));
        return true;
    }

    private static ItemStack hoveredTarget(GuiScreen screen) {
        ItemStack fromViewer = QuickCraftIntegrations.hoveredItem();
        if (!fromViewer.isEmpty()) return fromViewer;
        if (screen instanceof GuiContainer) {
            Slot slot = ((GuiContainer) screen).getSlotUnderMouse();
            if (slot != null && slot.getHasStack()) return slot.getStack();
        }
        return ItemStack.EMPTY;
    }
}
