package com.sxilverr.quickcraft.client;

import com.sxilverr.quickcraft.QuickCraftCommon;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = QuickCraftCommon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KEY);
        event.register(SCROLL_UP_KEY);
        event.register(SCROLL_DOWN_KEY);
        event.register(SHOW_RECIPE_KEY);
        event.register(SHOW_USES_KEY);
    }
}
