package com.sxilverr.quickcraft.crafting;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public interface RecipeOption {
    ResourceLocation id();

    ItemStack result();

    int resultCount();

    List<Ingredient> inputs();

    Station station();

    boolean fits(Stations stations);

    default boolean fitsInventory() {
        return false;
    }
}
