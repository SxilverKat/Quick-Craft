package com.sxilverr.quickcraft.crafting;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class CraftingRecipeOption implements RecipeOption {
    private final ResourceLocation id;
    private final IRecipe recipe;
    private final ItemStack result;
    private final List<Ingredient> inputs;

    public CraftingRecipeOption(ResourceLocation id, IRecipe recipe) {
        this.id = id;
        this.recipe = recipe;
        this.result = recipe.getRecipeOutput();
        this.inputs = new ArrayList<Ingredient>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (!Ingredients.isEmpty(ingredient)) inputs.add(ingredient);
        }
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public ItemStack result() {
        return result;
    }

    @Override
    public int resultCount() {
        return Math.max(1, result.getCount());
    }

    @Override
    public List<Ingredient> inputs() {
        return inputs;
    }

    @Override
    public Station station() {
        return Station.CRAFTING;
    }

    @Override
    public boolean fits(Stations stations) {
        return recipe.canFit(stations.gridSize(), stations.gridSize());
    }

    @Override
    public boolean fitsInventory() {
        return recipe.canFit(2, 2);
    }
}
