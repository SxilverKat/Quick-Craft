package com.sxilverr.quickcraft.integration.jei;

import com.sxilverr.quickcraft.QuickCraftCommon;
import com.sxilverr.quickcraft.integration.QuickCraftIntegrations;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocusFactory;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class QuickCraftJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(QuickCraftCommon.MODID, "jei");

    private IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        this.runtime = jeiRuntime;
        QuickCraftIntegrations.setHoveredItemProvider(this::hoveredItem);
        QuickCraftIntegrations.setRecipeViewer(this::showRecipe);
        QuickCraftIntegrations.setTextInputFocused(this::searchFocused);
    }

    @Override
    public void onRuntimeUnavailable() {
        this.runtime = null;
        QuickCraftIntegrations.setHoveredItemProvider(null);
        QuickCraftIntegrations.setRecipeViewer(null);
        QuickCraftIntegrations.setTextInputFocused(null);
    }

    private boolean searchFocused() {
        return runtime != null && runtime.getIngredientListOverlay().hasKeyboardFocus();
    }

    private void showRecipe(ItemStack stack, boolean uses) {
        if (runtime == null || stack == null || stack.isEmpty()) return;
        IFocusFactory focusFactory = runtime.getJeiHelpers().getFocusFactory();
        RecipeIngredientRole role = uses ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
        runtime.getRecipesGui().show(focusFactory.createFocus(role, VanillaTypes.ITEM_STACK, stack));
    }

    private ItemStack hoveredItem() {
        if (runtime == null) return ItemStack.EMPTY;
        if (runtime.getIngredientListOverlay().hasKeyboardFocus()) return ItemStack.EMPTY;

        ItemStack fromList = runtime.getIngredientListOverlay().getIngredientUnderMouse(VanillaTypes.ITEM_STACK);
        if (fromList != null && !fromList.isEmpty()) return fromList;

        ItemStack fromBookmark = runtime.getBookmarkOverlay().getItemStackUnderMouse();
        if (fromBookmark != null && !fromBookmark.isEmpty()) return fromBookmark;

        ItemStack fromScreen = ingredientUnderMouseInScreen();
        if (!fromScreen.isEmpty()) return fromScreen;

        return runtime.getRecipesGui().getIngredientUnderMouse(VanillaTypes.ITEM_STACK).orElse(ItemStack.EMPTY);
    }

    private ItemStack ingredientUnderMouseInScreen() {
        Minecraft mc = Minecraft.getInstance();
        Screen screen = mc.screen;
        if (screen == null) return ItemStack.EMPTY;
        double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight();
        return runtime.getScreenHelper().getClickableIngredientUnderMouse(screen, mx, my)
                .map(c -> c.getTypedIngredient().getItemStack().orElse(ItemStack.EMPTY))
                .filter(stack -> !stack.isEmpty())
                .findFirst()
                .orElse(ItemStack.EMPTY);
    }
}
