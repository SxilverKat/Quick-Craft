package com.sxilverr.quickcraft.crafting;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public class ModdedRecipeOption implements RecipeOption {
    private final ResourceLocation id;
    private final ItemStack result;
    private final List<Ingredient> inputs;
    private final Station station;

    public ModdedRecipeOption(ResourceLocation id, ItemStack result, List<Ingredient> inputs, Station station) {
        this.id = id;
        this.result = result;
        this.inputs = inputs;
        this.station = station;
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
        return station;
    }

    @Override
    public boolean fits(Stations stations) {
        return stations.has(station);
    }

    @Override
    public boolean fitsInventory() {
        return false;
    }
}
