package com.sxilverr.quickcraft.crafting;

public enum Station {
    CRAFTING("Crafting Table", "minecraft:crafting_table"),
    EXTREME_CRAFTING("Extreme Crafting Table", "avaritia:extreme_crafting_table");

    private final String displayName;
    private final String iconId;

    Station(String displayName, String iconId) {
        this.displayName = displayName;
        this.iconId = iconId;
    }

    public String displayName() {
        return displayName;
    }

    public String iconId() {
        return iconId;
    }
}
