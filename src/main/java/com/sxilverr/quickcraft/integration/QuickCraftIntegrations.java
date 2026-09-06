package com.sxilverr.quickcraft.integration;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BooleanSupplier;

public final class QuickCraftIntegrations {
    private static HoveredItemProvider hoveredItemProvider;
    private static RecipeViewer recipeViewer;
    private static OriginProvider originProvider;
    private static BooleanSupplier textInputFocused;

    private QuickCraftIntegrations() {
    }

    public static void setTextInputFocused(BooleanSupplier supplier) {
        textInputFocused = supplier;
    }

    public static boolean isTextInputFocused() {
        return textInputFocused != null && textInputFocused.getAsBoolean();
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

    public static void setOriginProvider(OriginProvider provider) {
        originProvider = provider;
    }

    public static boolean canFindOrigins() {
        return originProvider != null;
    }

    public static List<OriginHint> origins(ItemStack stack) {
        if (originProvider == null || stack == null || stack.isEmpty()) return List.of();
        List<OriginHint> hints = originProvider.find(stack);
        return hints == null ? List.of() : hints;
    }
}
