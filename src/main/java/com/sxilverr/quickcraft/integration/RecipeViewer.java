package com.sxilverr.quickcraft.integration;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface RecipeViewer {
    void show(ItemStack stack, boolean uses);
}
