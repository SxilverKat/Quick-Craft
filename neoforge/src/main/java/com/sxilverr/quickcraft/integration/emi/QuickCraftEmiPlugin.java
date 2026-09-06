package com.sxilverr.quickcraft.integration.emi;

import com.sxilverr.quickcraft.crafting.ItemOrigins;
import com.sxilverr.quickcraft.integration.OriginHint;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@EmiEntrypoint
public class QuickCraftEmiPlugin implements EmiPlugin {

    private static Set<EmiRecipeCategory> skipped() {
        return Set.of(
                VanillaEmiRecipeCategories.CRAFTING,
                VanillaEmiRecipeCategories.STONECUTTING,
                VanillaEmiRecipeCategories.SMITHING,
                VanillaEmiRecipeCategories.ANVIL_REPAIRING,
                VanillaEmiRecipeCategories.GRINDING,
                VanillaEmiRecipeCategories.FUEL,
                VanillaEmiRecipeCategories.COMPOSTING,
                VanillaEmiRecipeCategories.INFO);
    }

    @Override
    public void register(EmiRegistry registry) {
        ItemOrigins.invalidate();
        QuickCraftIntegrations.setHoveredItemProvider(QuickCraftEmiPlugin::hoveredItem);
        QuickCraftIntegrations.setRecipeViewer(QuickCraftEmiPlugin::showRecipe);
        QuickCraftIntegrations.setOriginProvider(QuickCraftEmiPlugin::findOrigins);
        QuickCraftIntegrations.setTextInputFocused(EmiApi::isSearchFocused);
    }

    private static ItemStack hoveredItem() {
        if (EmiApi.isSearchFocused()) return ItemStack.EMPTY;
        EmiStackInteraction hovered = EmiApi.getHoveredStack(true);
        if (hovered == null || hovered.isEmpty()) return ItemStack.EMPTY;
        return firstItem(hovered.getStack());
    }

    private static List<OriginHint> findOrigins(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        try {
            EmiRecipeManager manager = EmiApi.getRecipeManager();
            if (manager == null) return List.of();
            Set<EmiRecipeCategory> skip = skipped();
            Set<EmiRecipeCategory> seen = new LinkedHashSet<>();
            List<OriginHint> hints = new ArrayList<>();
            for (EmiRecipe recipe : manager.getRecipesByOutput(EmiStack.of(stack))) {
                EmiRecipeCategory category = recipe.getCategory();
                if (category == null || skip.contains(category) || !seen.add(category)) continue;
                ItemStack icon = ItemStack.EMPTY;
                for (EmiIngredient workstation : manager.getWorkstations(category)) {
                    icon = firstItem(workstation);
                    if (!icon.isEmpty()) break;
                }
                hints.add(new OriginHint(icon, category.getName().getString()));
            }
            return hints;
        } catch (Throwable t) {
            return List.of();
        }
    }

    private static ItemStack firstItem(EmiIngredient ingredient) {
        if (ingredient == null) return ItemStack.EMPTY;
        List<EmiStack> stacks = ingredient.getEmiStacks();
        if (stacks == null) return ItemStack.EMPTY;
        for (EmiStack stack : stacks) {
            ItemStack item = stack.getItemStack();
            if (!item.isEmpty()) return item;
        }
        return ItemStack.EMPTY;
    }

    private static void showRecipe(ItemStack stack, boolean uses) {
        if (stack == null || stack.isEmpty()) return;
        EmiIngredient ingredient = EmiStack.of(stack);
        if (uses) {
            EmiApi.displayUses(ingredient);
            return;
        }
        EmiApi.displayRecipes(ingredient);
    }
}
