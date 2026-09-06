package com.sxilverr.quickcraft.integration;

import net.minecraft.world.item.ItemStack;

import java.util.List;

@FunctionalInterface
public interface OriginProvider {
    List<OriginHint> find(ItemStack output);
}
