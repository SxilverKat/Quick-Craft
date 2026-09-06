package com.sxilverr.quickcraft.integration;

import net.minecraft.item.ItemStack;

import java.util.List;

public interface OriginProvider {
    List<OriginHint> find(ItemStack output);
}
