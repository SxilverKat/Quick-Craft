package com.sxilverr.quickcraft.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

public final class QuickCraftKeys {
    public static final String CATEGORY = "key.categories.quickcraft";

    public static final KeyBinding OPEN = new KeyBinding("key.quickcraft.open", Keyboard.KEY_G, CATEGORY);
    public static final KeyBinding SCROLL_UP = new KeyBinding("key.quickcraft.scroll_up", Keyboard.KEY_UP, CATEGORY);
    public static final KeyBinding SCROLL_DOWN = new KeyBinding("key.quickcraft.scroll_down", Keyboard.KEY_DOWN, CATEGORY);
    public static final KeyBinding SHOW_RECIPE = new KeyBinding("key.quickcraft.show_recipe", Keyboard.KEY_R, CATEGORY);
    public static final KeyBinding SHOW_USES = new KeyBinding("key.quickcraft.show_uses", Keyboard.KEY_U, CATEGORY);

    private QuickCraftKeys() {
    }

    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN);
        ClientRegistry.registerKeyBinding(SCROLL_UP);
        ClientRegistry.registerKeyBinding(SCROLL_DOWN);
        ClientRegistry.registerKeyBinding(SHOW_RECIPE);
        ClientRegistry.registerKeyBinding(SHOW_USES);
    }

    public static boolean matches(KeyBinding binding, int keyCode) {
        return binding.getKeyCode() == keyCode && keyCode != Keyboard.KEY_NONE;
    }
}
