package com.sxilverr.quickcraft.neoforge;

import com.sxilverr.quickcraft.client.QuickCraftClient;
import com.sxilverr.quickcraft.client.QuickCraftClientEvents;
import net.neoforged.bus.api.IEventBus;

public final class QuickCraftNeoForgeClient {
    private QuickCraftNeoForgeClient() {
    }

    public static void init(IEventBus modBus) {
        QuickCraftClient.init(modBus);
        QuickCraftClientEvents.init();
    }
}
