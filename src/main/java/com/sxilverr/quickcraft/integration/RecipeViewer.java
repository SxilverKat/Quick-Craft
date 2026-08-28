package com.sxilverr.quickcraft.integration;

import net.minecraft.item.ItemStack;

public interface RecipeViewer {
    void show(ItemStack stack, boolean uses);
}
