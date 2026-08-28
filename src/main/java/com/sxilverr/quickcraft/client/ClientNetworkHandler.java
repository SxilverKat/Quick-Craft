package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.craft.CraftPreview;
import com.sxilverr.quickcraft.crafting.ItemKey;
import com.sxilverr.quickcraft.crafting.Stations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Map;

public final class ClientNetworkHandler {
    private ClientNetworkHandler() {
    }

    public static void onAvailability(Map<ItemKey, Integer> counts, Map<ItemKey, ItemStack> sources,
                                      Map<ItemKey, ItemStack> samples, Stations stations) {
        BookmarkOverlay.setAvailability(counts);
        QuickCraftScreen screen = openScreen();
        if (screen != null) screen.setAvailability(counts, sources, samples, stations);
    }

    public static void onCraftPreview(int craftable, int requested, List<CraftPreview.Gain> gained) {
        QuickCraftScreen screen = openScreen();
        if (screen != null) {
            screen.onCraftPreviewResult(new CraftPreview.Result(craftable, requested, gained));
        }
    }

    private static QuickCraftScreen openScreen() {
        GuiScreen current = Minecraft.getMinecraft().currentScreen;
        return current instanceof QuickCraftScreen ? (QuickCraftScreen) current : null;
    }
}
