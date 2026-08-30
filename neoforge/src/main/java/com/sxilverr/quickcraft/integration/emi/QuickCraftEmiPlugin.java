package com.sxilverr.quickcraft.integration.emi;

import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@EmiEntrypoint
public class QuickCraftEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        QuickCraftIntegrations.setHoveredItemProvider(QuickCraftEmiPlugin::hoveredItem);
        QuickCraftIntegrations.setRecipeViewer(QuickCraftEmiPlugin::showRecipe);
        QuickCraftIntegrations.setTextInputFocused(EmiApi::isSearchFocused);
    }

    private static ItemStack hoveredItem() {
        if (EmiApi.isSearchFocused()) return ItemStack.EMPTY;
        EmiStackInteraction hovered = EmiApi.getHoveredStack(true);
        if (hovered == null || hovered.isEmpty()) return ItemStack.EMPTY;
        return firstItem(hovered.getStack());
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
