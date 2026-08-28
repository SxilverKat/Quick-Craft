package com.sxilverr.quickcraft.integration.jei;

import com.sxilverr.quickcraft.integration.HoveredItemProvider;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import com.sxilverr.quickcraft.integration.RecipeViewer;
import com.sxilverr.quickcraft.integration.TextInputFocus;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IFocus;
import net.minecraft.item.ItemStack;

@JEIPlugin
public class QuickCraftJeiPlugin implements IModPlugin {
    private IJeiRuntime runtime;

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.runtime = jeiRuntime;
        QuickCraftIntegrations.setHoveredItemProvider(new HoveredItemProvider() {
            @Override
            public ItemStack getHoveredItem() {
                return hoveredItem();
            }
        });
        QuickCraftIntegrations.setRecipeViewer(new RecipeViewer() {
            @Override
            public void show(ItemStack stack, boolean uses) {
                showRecipe(stack, uses);
            }
        });
        QuickCraftIntegrations.setTextInputFocused(new TextInputFocus() {
            @Override
            public boolean isFocused() {
                return searchFocused();
            }
        });
    }

    private boolean searchFocused() {
        try {
            return runtime != null && runtime.getIngredientListOverlay().hasKeyboardFocus();
        } catch (Throwable t) {
            return false;
        }
    }

    private void showRecipe(ItemStack stack, boolean uses) {
        if (runtime == null || stack == null || stack.isEmpty()) return;
        try {
            IFocus.Mode mode = uses ? IFocus.Mode.INPUT : IFocus.Mode.OUTPUT;
            runtime.getRecipesGui().show(runtime.getRecipeRegistry().createFocus(mode, stack));
        } catch (Throwable t) {
        }
    }

    private ItemStack hoveredItem() {
        if (runtime == null) return ItemStack.EMPTY;
        try {
            if (runtime.getIngredientListOverlay().hasKeyboardFocus()) return ItemStack.EMPTY;

            ItemStack fromList = asStack(runtime.getIngredientListOverlay().getIngredientUnderMouse());
            if (!fromList.isEmpty()) return fromList;

            ItemStack fromBookmark = asStack(runtime.getBookmarkOverlay().getIngredientUnderMouse());
            if (!fromBookmark.isEmpty()) return fromBookmark;
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack asStack(Object ingredient) {
        return ingredient instanceof ItemStack ? (ItemStack) ingredient : ItemStack.EMPTY;
    }
}
