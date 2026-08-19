package com.sxilverr.quickcraft.client;

import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class QuickCraftClient {
    public static final KeyMapping OPEN_KEY = new KeyMapping(
            "key.quickcraft.open", GLFW.GLFW_KEY_G, "key.categories.quickcraft");
    public static final KeyMapping SCROLL_UP_KEY = new KeyMapping(
            "key.quickcraft.scroll_up", GLFW.GLFW_KEY_UP, "key.categories.quickcraft");
    public static final KeyMapping SCROLL_DOWN_KEY = new KeyMapping(
            "key.quickcraft.scroll_down", GLFW.GLFW_KEY_DOWN, "key.categories.quickcraft");
    public static final KeyMapping SHOW_RECIPE_KEY = new KeyMapping(
            "key.quickcraft.show_recipe", GLFW.GLFW_KEY_R, "key.categories.quickcraft");
    public static final KeyMapping SHOW_USES_KEY = new KeyMapping(
            "key.quickcraft.show_uses", GLFW.GLFW_KEY_U, "key.categories.quickcraft");

    private QuickCraftClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(QuickCraftClient::onRegisterKeyMappings);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KEY);
        event.register(SCROLL_UP_KEY);
        event.register(SCROLL_DOWN_KEY);
        event.register(SHOW_RECIPE_KEY);
        event.register(SHOW_USES_KEY);
    }
}
