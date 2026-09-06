package com.sxilverr.quickcraft.integration;

import net.minecraft.item.ItemStack;

public final class OriginHint {
    private final ItemStack icon;
    private final String label;

    public OriginHint(ItemStack icon, String label) {
        this.icon = icon;
        this.label = label;
    }

    public ItemStack icon() {
        return icon;
    }

    public String label() {
        return label;
    }
}
