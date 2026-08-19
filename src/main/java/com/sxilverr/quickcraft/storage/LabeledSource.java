package com.sxilverr.quickcraft.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public record LabeledSource(String id, String name, ItemStack icon, BlockPos pos, ItemSource source,
                            boolean depositable) {
    public static final String SELF_ID = "self";

    public boolean isSelf() {
        return SELF_ID.equals(id);
    }

    public String pickerLabel() {
        if (pos == null) return name;
        return name + " (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")";
    }
}
