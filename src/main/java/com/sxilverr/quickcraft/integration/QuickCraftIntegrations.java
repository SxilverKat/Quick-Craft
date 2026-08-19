package com.sxilverr.quickcraft.integration;

import net.minecraft.world.item.ItemStack;

public final class QuickCraftIntegrations {
    private static HoveredItemProvider hoveredItemProvider;
    private static RecipeViewer recipeViewer;

    private QuickCraftIntegrations() {
    }

    public static void setHoveredItemProvider(HoveredItemProvider provider) {
        hoveredItemProvider = provider;
    }

    public static ItemStack hoveredItem() {
        if (hoveredItemProvider == null) return ItemStack.EMPTY;
        ItemStack stack = hoveredItemProvider.getHoveredItem();
        return stack == null ? ItemStack.EMPTY : stack;
    }

    public static void setRecipeViewer(RecipeViewer viewer) {
        recipeViewer = viewer;
    }

    public static boolean canShowRecipes() {
        return recipeViewer != null;
    }

    public static void showRecipe(ItemStack stack) {
        if (recipeViewer != null && stack != null && !stack.isEmpty()) recipeViewer.show(stack, false);
    }

    public static void showUses(ItemStack stack) {
        if (recipeViewer != null && stack != null && !stack.isEmpty()) recipeViewer.show(stack, true);
    }
}
