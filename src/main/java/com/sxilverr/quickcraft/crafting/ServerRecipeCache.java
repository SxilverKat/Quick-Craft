package com.sxilverr.quickcraft.crafting;

public final class ServerRecipeCache {
    private static RecipeResolver resolver;

    private ServerRecipeCache() {
    }

    public static RecipeResolver get() {
        if (resolver == null) resolver = new RecipeResolver();
        return resolver;
    }

    public static void clear() {
        resolver = null;
    }
}
