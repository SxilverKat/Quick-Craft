package com.sxilverr.quickcraft.integration;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface HoveredItemProvider {
    ItemStack getHoveredItem();
}
