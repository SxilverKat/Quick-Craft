package com.sxilverr.quickcraft.crafting;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.RecipeManager;

public final class ServerRecipeCache {
    private static RecipeResolver resolver;
    private static RecipeManager boundManager;

    private ServerRecipeCache() {
    }

    public static RecipeResolver get(RecipeManager manager, RegistryAccess registryAccess) {
        if (resolver == null || boundManager != manager) {
            resolver = new RecipeResolver(manager, registryAccess);
            boundManager = manager;
        }
        return resolver;
    }

    public static void clear() {
        resolver = null;
        boundManager = null;
    }
}
