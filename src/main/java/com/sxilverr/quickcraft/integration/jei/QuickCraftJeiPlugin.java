package com.sxilverr.quickcraft.integration.jei;

import com.sxilverr.quickcraft.integration.HoveredItemProvider;
import com.sxilverr.quickcraft.integration.OriginHint;
import com.sxilverr.quickcraft.integration.OriginProvider;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import com.sxilverr.quickcraft.integration.RecipeViewer;
import com.sxilverr.quickcraft.integration.TextInputFocus;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IRecipeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JEIPlugin
public class QuickCraftJeiPlugin implements IModPlugin {
    private static final Set<String> SKIP_CATEGORIES = new HashSet<String>(Arrays.asList(
            VanillaRecipeCategoryUid.CRAFTING,
            VanillaRecipeCategoryUid.FUEL,
            VanillaRecipeCategoryUid.ANVIL,
            VanillaRecipeCategoryUid.INFORMATION));

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
        QuickCraftIntegrations.setOriginProvider(new OriginProvider() {
            @Override
            public List<OriginHint> find(ItemStack output) {
                return findOrigins(output);
            }
        });
        QuickCraftIntegrations.setTextInputFocused(new TextInputFocus() {
            @Override
            public boolean isFocused() {
                return searchFocused();
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private List<OriginHint> findOrigins(ItemStack stack) {
        if (runtime == null || stack == null || stack.isEmpty()) return Collections.emptyList();
        try {
            IRecipeRegistry registry = runtime.getRecipeRegistry();
            IFocus<ItemStack> focus = registry.createFocus(IFocus.Mode.OUTPUT, stack);
            List<OriginHint> hints = new ArrayList<OriginHint>();
            for (IRecipeCategory category : registry.getRecipeCategories(focus)) {
                if (category == null || SKIP_CATEGORIES.contains(category.getUid())) continue;
                ItemStack icon = ItemStack.EMPTY;
                for (Object catalyst : registry.getRecipeCatalysts(category)) {
                    ItemStack candidate = asStack(catalyst);
                    if (!candidate.isEmpty()) {
                        icon = candidate;
                        break;
                    }
                }
                hints.add(new OriginHint(icon, category.getTitle()));
            }
            return hints;
        } catch (Throwable t) {
            return Collections.emptyList();
        }
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

            ItemStack fromRecipes = asStack(runtime.getRecipesGui().getIngredientUnderMouse());
            if (!fromRecipes.isEmpty()) return fromRecipes;
        } catch (Throwable t) {
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack asStack(Object ingredient) {
        return ingredient instanceof ItemStack ? (ItemStack) ingredient : ItemStack.EMPTY;
    }
}
