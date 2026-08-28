package com.sxilverr.quickcraft.crafting;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;

import java.util.List;

public interface RecipeOption {
    ResourceLocation id();

    ItemStack result();

    int resultCount();

    List<Ingredient> inputs();

    Station station();

    boolean fits(Stations stations);

    boolean fitsInventory();
}
