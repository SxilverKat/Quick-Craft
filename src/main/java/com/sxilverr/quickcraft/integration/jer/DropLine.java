package com.sxilverr.quickcraft.integration.jer;

import net.minecraft.item.ItemStack;

public final class DropLine {
    public final ItemStack item;
    public final int min;
    public final int max;
    public final boolean looting;
    public final String chanceLabel;

    public DropLine(ItemStack item, int min, int max, boolean looting, String chanceLabel) {
        this.item = item;
        this.min = min;
        this.max = max;
        this.looting = looting;
        this.chanceLabel = chanceLabel == null ? "" : chanceLabel;
    }

    public String rangeLabel() {
        return min == max ? Integer.toString(min) : min + "-" + max;
    }
}
